package com.uca.cfc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.catering.ServicioCateringRequest;
import com.uca.cfc.dto.catering.ServicioCateringResponse;
import com.uca.cfc.dto.catering.SolicitudCateringRequest;
import com.uca.cfc.dto.catering.SolicitudCateringResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.ServicioCatering;
import com.uca.cfc.entity.SolicitudCatering;
import com.uca.cfc.entity.enums.EstadoReserva;
import com.uca.cfc.entity.enums.TipoActividad;
import com.uca.cfc.entity.enums.TipoServicioCatering;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.DuplicateResourceException;
import com.uca.cfc.exception.ResourceInUseException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.CateringMapper;
import com.uca.cfc.repository.ServicioCateringRepository;
import com.uca.cfc.repository.SolicitudCateringRepository;
import com.uca.cfc.util.IntervaloHorario;

/** Catálogo de servicios de catering y solicitudes de los clientes. */
@Service
@Transactional(readOnly = true)
public class CateringService {

    private static final String RECURSO_SERVICIO = "Servicio de catering";
    private static final String RECURSO_SOLICITUD = "Solicitud de catering";

    private final ServicioCateringRepository servicioRepository;
    private final SolicitudCateringRepository solicitudRepository;
    private final ClienteService clienteService;
    private final EspacioService espacioService;
    private final AgendaService agendaService;
    private final CateringMapper mapper;

    public CateringService(ServicioCateringRepository servicioRepository,
            SolicitudCateringRepository solicitudRepository, ClienteService clienteService,
            EspacioService espacioService, AgendaService agendaService, CateringMapper mapper) {
        this.servicioRepository = servicioRepository;
        this.solicitudRepository = solicitudRepository;
        this.clienteService = clienteService;
        this.espacioService = espacioService;
        this.agendaService = agendaService;
        this.mapper = mapper;
    }

    // ----------------------------- Servicios -----------------------------

    public PageResponse<ServicioCateringResponse> listarServicios(TipoServicioCatering tipo, Boolean activo,
            Pageable pageable) {
        return PageResponse.de(servicioRepository.buscar(tipo, activo, pageable), mapper::aResponse);
    }

    public ServicioCateringResponse obtenerServicio(Long id) {
        return mapper.aResponse(buscarServicio(id));
    }

    @Transactional
    public ServicioCateringResponse crearServicio(ServicioCateringRequest request) {
        if (servicioRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new DuplicateResourceException(
                    "Ya existe un servicio de catering con el nombre '" + request.nombre() + "'");
        }
        ServicioCatering servicio = new ServicioCatering();
        aplicar(request, servicio);
        return mapper.aResponse(servicioRepository.save(servicio));
    }

    @Transactional
    public ServicioCateringResponse actualizarServicio(Long id, ServicioCateringRequest request) {
        ServicioCatering servicio = buscarServicio(id);
        if (servicioRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre().trim(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe un servicio de catering con el nombre '" + request.nombre() + "'");
        }
        aplicar(request, servicio);
        return mapper.aResponse(servicioRepository.save(servicio));
    }

    @Transactional
    public void eliminarServicio(Long id) {
        ServicioCatering servicio = buscarServicio(id);
        if (solicitudRepository.existsByServicioId(id)) {
            throw new ResourceInUseException("No se puede eliminar el servicio '" + servicio.getNombre()
                    + "' porque tiene solicitudes registradas. Desactívelo en su lugar");
        }
        servicioRepository.delete(servicio);
    }

    // ---------------------------- Solicitudes ----------------------------

    public PageResponse<SolicitudCateringResponse> listarSolicitudes(Long clienteId, Long servicioId,
            EstadoReserva estado, LocalDate desde, LocalDate hasta, Pageable pageable) {
        return PageResponse.de(solicitudRepository.buscar(clienteId, servicioId, estado, desde, hasta, pageable),
                mapper::aResponse);
    }

    public SolicitudCateringResponse obtenerSolicitud(Long id) {
        return mapper.aResponse(buscarSolicitud(id));
    }

    @Transactional
    public SolicitudCateringResponse crearSolicitud(SolicitudCateringRequest request) {
        SolicitudCatering solicitud = new SolicitudCatering();
        aplicar(request, solicitud);
        solicitud.setEstado(EstadoReserva.PENDIENTE);

        SolicitudCatering guardada = solicitudRepository.save(solicitud);
        sincronizarAgenda(guardada);
        return mapper.aResponse(guardada);
    }

    @Transactional
    public SolicitudCateringResponse actualizarSolicitud(Long id, SolicitudCateringRequest request) {
        SolicitudCatering solicitud = buscarSolicitud(id);
        if (solicitud.getEstado().esFinal()) {
            throw new BusinessException("No se puede modificar una solicitud en estado " + solicitud.getEstado());
        }
        aplicar(request, solicitud);

        SolicitudCatering actualizada = solicitudRepository.save(solicitud);
        sincronizarAgenda(actualizada);
        return mapper.aResponse(actualizada);
    }

    @Transactional
    public SolicitudCateringResponse cambiarEstadoSolicitud(Long id, EstadoReserva destino, String motivo) {
        SolicitudCatering solicitud = buscarSolicitud(id);
        EstadoReserva actual = solicitud.getEstado();

        if (actual == destino) {
            return mapper.aResponse(solicitud);
        }
        if (!actual.puedeCambiarA(destino)) {
            throw new BusinessException("No es posible pasar la solicitud de " + actual + " a " + destino);
        }

        solicitud.setEstado(destino);
        if (motivo != null && !motivo.isBlank()) {
            solicitud.setObservaciones(motivo.trim());
        }
        SolicitudCatering actualizada = solicitudRepository.save(solicitud);

        if (destino == EstadoReserva.CANCELADA) {
            agendaService.eliminarSincronizada(TipoActividad.CATERING, id);
        } else {
            sincronizarAgenda(actualizada);
        }
        return mapper.aResponse(actualizada);
    }

    @Transactional
    public void eliminarSolicitud(Long id) {
        SolicitudCatering solicitud = buscarSolicitud(id);
        agendaService.eliminarSincronizada(TipoActividad.CATERING, id);
        solicitudRepository.delete(solicitud);
    }

    public ServicioCatering buscarServicio(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_SERVICIO, id));
    }

    public SolicitudCatering buscarSolicitud(Long id) {
        return solicitudRepository.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_SOLICITUD, id));
    }

    private void aplicar(ServicioCateringRequest request, ServicioCatering servicio) {
        servicio.setNombre(request.nombre().trim());
        servicio.setTipo(request.tipo());
        servicio.setDescripcion(request.descripcion());
        servicio.setPrecioPorPersona(request.precioPorPersona());
        servicio.setMinimoPersonas(request.minimoPersonas());
        if (request.activo() != null) {
            servicio.setActivo(request.activo());
        }
    }

    private void aplicar(SolicitudCateringRequest request, SolicitudCatering solicitud) {
        IntervaloHorario intervalo = IntervaloHorario.of(request.fecha(), request.horaInicio(), request.fecha(),
                request.horaFin());

        ServicioCatering servicio = buscarServicio(request.servicioId());
        if (!servicio.isActivo()) {
            throw new BusinessException("El servicio '" + servicio.getNombre() + "' está inactivo");
        }
        if (request.cantidadAsistentes() < servicio.getMinimoPersonas()) {
            throw new BusinessException("El servicio '" + servicio.getNombre() + "' requiere un mínimo de "
                    + servicio.getMinimoPersonas() + " personas");
        }

        Espacio espacio = request.espacioId() == null ? null : espacioService.buscar(request.espacioId());
        String lugar = resolverLugar(request, espacio);
        if (espacio != null && request.cantidadAsistentes() > espacio.getCapacidad()) {
            throw new BusinessException("El espacio '" + espacio.getNombre() + "' tiene capacidad para "
                    + espacio.getCapacidad() + " personas y se solicitaron " + request.cantidadAsistentes());
        }

        solicitud.setCliente(clienteService.buscar(request.clienteId()));
        solicitud.setServicio(servicio);
        solicitud.setEspacio(espacio);
        solicitud.setLugar(lugar);
        solicitud.setCantidadAsistentes(request.cantidadAsistentes());
        solicitud.setMenu(request.menu());
        solicitud.setFecha(intervalo.inicio().toLocalDate());
        solicitud.setHoraInicio(intervalo.inicio().toLocalTime());
        solicitud.setHoraFin(intervalo.fin().toLocalTime());
        solicitud.setMontoTotal(calcularMonto(servicio, request.cantidadAsistentes()));
        solicitud.setObservaciones(request.observaciones());
    }

    private String resolverLugar(SolicitudCateringRequest request, Espacio espacio) {
        if (request.lugar() != null && !request.lugar().isBlank()) {
            return request.lugar().trim();
        }
        if (espacio != null) {
            return espacio.getNombre();
        }
        throw new BusinessException("Indique el lugar del servicio o seleccione un espacio del centro");
    }

    private BigDecimal calcularMonto(ServicioCatering servicio, int asistentes) {
        return servicio.getPrecioPorPersona().multiply(BigDecimal.valueOf(asistentes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * El catering solo entra al calendario cuando ocurre en un espacio del
     * centro; si se sirve en una sede externa no ocupa la agenda institucional.
     */
    private void sincronizarAgenda(SolicitudCatering solicitud) {
        if (solicitud.getEspacio() == null) {
            agendaService.eliminarSincronizada(TipoActividad.CATERING, solicitud.getId());
            return;
        }
        agendaService.sincronizar(TipoActividad.CATERING, solicitud.getId(),
                solicitud.getServicio().getNombre() + " · " + solicitud.getCliente().getNombre(),
                solicitud.getEspacio(), solicitud.getFechaHoraInicio(), solicitud.getFechaHoraFin(),
                "Servicio de catering para " + solicitud.getCantidadAsistentes() + " personas",
                solicitud.getCliente().getNombre());
    }
}
