package com.uca.cfc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.alquiler.AlquilerRequest;
import com.uca.cfc.dto.alquiler.AlquilerResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.Alquiler;
import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.EstadoReserva;
import com.uca.cfc.entity.enums.TipoActividad;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.EspacioOcupadoException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.AlquilerMapper;
import com.uca.cfc.repository.AlquilerRepository;
import com.uca.cfc.repository.EspacioRepository;
import com.uca.cfc.util.IntervaloHorario;

/**
 * Regla crítica del módulo: nunca reservar un espacio cuando ya existe una
 * actividad que se superpone temporalmente. La comprobación se hace en esta capa,
 * dentro de una transacción y con la fila del espacio bloqueada.
 */
@Service
@Transactional(readOnly = true)
public class AlquilerService {

    private static final String RECURSO = "Alquiler";
    private static final BigDecimal MINUTOS_POR_HORA = BigDecimal.valueOf(60);

    private static final Logger log = LoggerFactory.getLogger(AlquilerService.class);

    private final AlquilerRepository alquilerRepository;
    private final EspacioRepository espacioRepository;
    private final ClienteService clienteService;
    private final AgendaService agendaService;
    private final AlquilerMapper mapper;

    public AlquilerService(AlquilerRepository alquilerRepository, EspacioRepository espacioRepository,
            ClienteService clienteService, AgendaService agendaService, AlquilerMapper mapper) {
        this.alquilerRepository = alquilerRepository;
        this.espacioRepository = espacioRepository;
        this.clienteService = clienteService;
        this.agendaService = agendaService;
        this.mapper = mapper;
    }

    public PageResponse<AlquilerResponse> listar(Long clienteId, Long espacioId, EstadoReserva estado,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta, Pageable pageable) {
        return PageResponse.de(alquilerRepository.buscar(clienteId, espacioId, estado, desde, hasta, pageable),
                mapper::aResponse);
    }

    public AlquilerResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public AlquilerResponse crear(AlquilerRequest request) {
        IntervaloHorario intervalo = new IntervaloHorario(request.fechaHoraInicio(), request.fechaHoraFin());
        Espacio espacio = bloquear(request.espacioId());

        validarEspacioUtilizable(espacio);
        validarCapacidad(espacio, request.cantidadAsistentes());
        validarSinConflictos(espacio, intervalo, null);

        Alquiler alquiler = new Alquiler();
        alquiler.setCliente(clienteService.buscar(request.clienteId()));
        alquiler.setEspacio(espacio);
        alquiler.setFechaHoraInicio(intervalo.inicio());
        alquiler.setFechaHoraFin(intervalo.fin());
        alquiler.setProposito(request.proposito().trim());
        alquiler.setCantidadAsistentes(request.cantidadAsistentes());
        alquiler.setMontoTotal(calcularMonto(espacio, intervalo));
        alquiler.setEstado(EstadoReserva.PENDIENTE);
        alquiler.setObservaciones(request.observaciones());

        Alquiler guardado = alquilerRepository.save(alquiler);
        sincronizarAgenda(guardado);
        log.info("Alquiler {} creado para el espacio '{}'", guardado.getId(), espacio.getNombre());
        return mapper.aResponse(guardado);
    }

    @Transactional
    public AlquilerResponse actualizar(Long id, AlquilerRequest request) {
        Alquiler alquiler = buscar(id);
        if (alquiler.getEstado().esFinal()) {
            throw new BusinessException("No se puede modificar un alquiler en estado " + alquiler.getEstado());
        }

        IntervaloHorario intervalo = new IntervaloHorario(request.fechaHoraInicio(), request.fechaHoraFin());
        Espacio espacio = bloquear(request.espacioId());

        validarEspacioUtilizable(espacio);
        validarCapacidad(espacio, request.cantidadAsistentes());
        validarSinConflictos(espacio, intervalo, id);

        alquiler.setCliente(clienteService.buscar(request.clienteId()));
        alquiler.setEspacio(espacio);
        alquiler.setFechaHoraInicio(intervalo.inicio());
        alquiler.setFechaHoraFin(intervalo.fin());
        alquiler.setProposito(request.proposito().trim());
        alquiler.setCantidadAsistentes(request.cantidadAsistentes());
        alquiler.setMontoTotal(calcularMonto(espacio, intervalo));
        alquiler.setObservaciones(request.observaciones());

        Alquiler actualizado = alquilerRepository.save(alquiler);
        sincronizarAgenda(actualizado);
        return mapper.aResponse(actualizado);
    }

    @Transactional
    public AlquilerResponse cambiarEstado(Long id, EstadoReserva destino, String motivo) {
        Alquiler alquiler = buscar(id);
        EstadoReserva actual = alquiler.getEstado();

        if (actual == destino) {
            return mapper.aResponse(alquiler);
        }
        if (!actual.puedeCambiarA(destino)) {
            throw new BusinessException("No es posible pasar el alquiler de " + actual + " a " + destino);
        }

        alquiler.setEstado(destino);
        if (motivo != null && !motivo.isBlank()) {
            alquiler.setObservaciones(motivo.trim());
        }
        Alquiler actualizado = alquilerRepository.save(alquiler);

        // Un alquiler cancelado libera el espacio y desaparece del calendario
        if (destino == EstadoReserva.CANCELADA) {
            agendaService.eliminarSincronizada(TipoActividad.ALQUILER, id);
        } else {
            sincronizarAgenda(actualizado);
        }
        log.info("Alquiler {}: estado {} → {}", id, actual, destino);
        return mapper.aResponse(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        Alquiler alquiler = buscar(id);
        agendaService.eliminarSincronizada(TipoActividad.ALQUILER, id);
        alquilerRepository.delete(alquiler);
    }

    public Alquiler buscar(Long id) {
        return alquilerRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    /**
     * Traslape: nuevoInicio &lt; existenteFin AND nuevoFin &gt; existenteInicio.
     */
    private void validarSinConflictos(Espacio espacio, IntervaloHorario intervalo, Long idExcluido) {
        List<Alquiler> conflictos = alquilerRepository.buscarSolapamientos(espacio.getId(), intervalo.inicio(),
                intervalo.fin(), idExcluido);
        if (!conflictos.isEmpty()) {
            Alquiler conflicto = conflictos.getFirst();
            throw new EspacioOcupadoException(espacio.getNombre(), conflicto.getProposito(),
                    conflicto.getFechaHoraInicio(), conflicto.getFechaHoraFin());
        }

        agendaService.buscarConflictoDeEspacio(espacio.getId(), intervalo, TipoActividad.ALQUILER, idExcluido)
                .ifPresent(actividad -> {
                    throw new EspacioOcupadoException(espacio.getNombre(), actividad.getTitulo(),
                            actividad.getFechaHoraInicio(), actividad.getFechaHoraFin());
                });
    }

    private void validarEspacioUtilizable(Espacio espacio) {
        if (espacio.getEstado() != EstadoEspacio.DISPONIBLE) {
            throw new BusinessException("El espacio '" + espacio.getNombre() + "' está en estado "
                    + espacio.getEstado() + " y no admite reservas");
        }
    }

    private void validarCapacidad(Espacio espacio, int asistentes) {
        if (asistentes > espacio.getCapacidad()) {
            throw new BusinessException("El espacio '" + espacio.getNombre() + "' tiene capacidad para "
                    + espacio.getCapacidad() + " personas y se solicitaron " + asistentes);
        }
    }

    /** El monto se cobra por hora, prorrateado a los minutos efectivamente reservados. */
    private BigDecimal calcularMonto(Espacio espacio, IntervaloHorario intervalo) {
        BigDecimal horas = BigDecimal.valueOf(intervalo.duracion().toMinutes())
                .divide(MINUTOS_POR_HORA, 4, RoundingMode.HALF_UP);
        return espacio.getPrecioPorHora().multiply(horas).setScale(2, RoundingMode.HALF_UP);
    }

    private void sincronizarAgenda(Alquiler alquiler) {
        agendaService.sincronizar(TipoActividad.ALQUILER, alquiler.getId(),
                alquiler.getProposito() + " · " + alquiler.getCliente().getNombre(), alquiler.getEspacio(),
                alquiler.getFechaHoraInicio(), alquiler.getFechaHoraFin(),
                "Alquiler de " + alquiler.getEspacio().getNombre(), alquiler.getCliente().getNombre());
    }

    private Espacio bloquear(Long espacioId) {
        return espacioRepository.findForUpdateById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio", espacioId));
    }
}
