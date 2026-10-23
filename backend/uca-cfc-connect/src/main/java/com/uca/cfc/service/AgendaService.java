package com.uca.cfc.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.agenda.ActividadAgendaRequest;
import com.uca.cfc.dto.agenda.ActividadAgendaResponse;
import com.uca.cfc.dto.agenda.ConflictoAgendaResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.ActividadAgenda;
import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.enums.TipoActividad;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.mapper.AgendaMapper;
import com.uca.cfc.repository.ActividadAgendaRepository;
import com.uca.cfc.util.IntervaloHorario;

/**
 * Calendario institucional. Las actividades de alquileres y catering se
 * sincronizan desde sus módulos mediante {@link #sincronizar} y no se editan aquí.
 */
@Service
@Transactional(readOnly = true)
public class AgendaService {

    private static final String RECURSO = "Actividad de agenda";

    private final ActividadAgendaRepository agendaRepository;
    private final EspacioService espacioService;
    private final AgendaMapper mapper;

    public AgendaService(ActividadAgendaRepository agendaRepository, EspacioService espacioService,
            AgendaMapper mapper) {
        this.agendaRepository = agendaRepository;
        this.espacioService = espacioService;
        this.mapper = mapper;
    }

    public PageResponse<ActividadAgendaResponse> listar(TipoActividad tipo, Long espacioId, LocalDateTime desde,
            LocalDateTime hasta, Pageable pageable) {
        return PageResponse.de(agendaRepository.buscar(tipo, espacioId, desde, hasta, pageable), mapper::aResponse);
    }

    public ActividadAgendaResponse obtener(Long id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public ActividadAgendaResponse crear(ActividadAgendaRequest request) {
        validarTipoEditable(request.tipo());
        ActividadAgenda actividad = new ActividadAgenda();
        aplicar(request, actividad);
        return mapper.aResponse(agendaRepository.save(actividad));
    }

    @Transactional
    public ActividadAgendaResponse actualizar(Long id, ActividadAgendaRequest request) {
        ActividadAgenda actividad = buscar(id);
        if (actividad.getTipo().esSincronizado()) {
            throw new BusinessException("Esta actividad proviene del módulo de "
                    + actividad.getTipo().name().toLowerCase() + " y se modifica desde ahí");
        }
        validarTipoEditable(request.tipo());
        aplicar(request, actividad);
        return mapper.aResponse(agendaRepository.save(actividad));
    }

    @Transactional
    public void eliminar(Long id) {
        ActividadAgenda actividad = buscar(id);
        if (actividad.getTipo().esSincronizado()) {
            throw new BusinessException("Esta actividad proviene del módulo de "
                    + actividad.getTipo().name().toLowerCase() + " y se elimina desde ahí");
        }
        agendaRepository.delete(actividad);
    }

    /**
     * Detecta traslapes comparando, dentro de cada espacio, cada actividad con
     * las siguientes del mismo espacio. La lista llega ordenada por espacio y
     * hora de inicio, así que basta con cortar cuando una actividad ya no se
     * superpone con la actual.
     */
    public List<ConflictoAgendaResponse> detectarConflictos(LocalDateTime desde, LocalDateTime hasta) {
        List<ActividadAgenda> actividades = agendaRepository.findParaAnalisisDeConflictos(desde, hasta);
        List<ConflictoAgendaResponse> conflictos = new ArrayList<>();

        for (int i = 0; i < actividades.size(); i++) {
            ActividadAgenda actual = actividades.get(i);
            for (int j = i + 1; j < actividades.size(); j++) {
                ActividadAgenda siguiente = actividades.get(j);
                if (!siguiente.getEspacio().getId().equals(actual.getEspacio().getId())) {
                    break;
                }
                if (!siguiente.getFechaHoraInicio().isBefore(actual.getFechaHoraFin())) {
                    break;
                }
                conflictos.add(construirConflicto(actual, siguiente));
            }
        }
        return conflictos;
    }

    /**
     * Primera actividad del calendario que choca con el intervalo dado en ese
     * espacio. Se ignora la actividad generada por el propio origen que se está
     * guardando, para que actualizar un alquiler no choque consigo mismo.
     */
    public Optional<ActividadAgenda> buscarConflictoDeEspacio(Long espacioId, IntervaloHorario intervalo,
            TipoActividad tipoOrigen, Long origenId) {
        return agendaRepository.buscarSolapamientos(espacioId, intervalo.inicio(), intervalo.fin(), null).stream()
                .filter(actividad -> !(actividad.getTipo() == tipoOrigen && origenId != null
                        && origenId.equals(actividad.getOrigenId())))
                .findFirst();
    }

    /**
     * Crea o actualiza la entrada de agenda de un alquiler o de una solicitud de
     * catering. Un origen genera una sola actividad.
     */
    @Transactional
    public void sincronizar(TipoActividad tipo, Long origenId, String titulo, Espacio espacio,
            LocalDateTime inicio, LocalDateTime fin, String descripcion, String responsable) {
        ActividadAgenda actividad = agendaRepository.findByTipoAndOrigenId(tipo, origenId)
                .orElseGet(ActividadAgenda::new);
        actividad.setTipo(tipo);
        actividad.setOrigenId(origenId);
        actividad.setTitulo(titulo);
        actividad.setEspacio(espacio);
        actividad.setFechaHoraInicio(inicio);
        actividad.setFechaHoraFin(fin);
        actividad.setDescripcion(descripcion);
        actividad.setResponsable(responsable);
        agendaRepository.save(actividad);
    }

    /** Quita de la agenda la actividad generada por un origen que se canceló o eliminó. */
    @Transactional
    public void eliminarSincronizada(TipoActividad tipo, Long origenId) {
        agendaRepository.findByTipoAndOrigenId(tipo, origenId).ifPresent(agendaRepository::delete);
    }

    public ActividadAgenda buscar(Long id) {
        return agendaRepository.findWithEspacioById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private ConflictoAgendaResponse construirConflicto(ActividadAgenda a, ActividadAgenda b) {
        LocalDateTime desde = a.getFechaHoraInicio().isAfter(b.getFechaHoraInicio())
                ? a.getFechaHoraInicio() : b.getFechaHoraInicio();
        LocalDateTime hasta = a.getFechaHoraFin().isBefore(b.getFechaHoraFin())
                ? a.getFechaHoraFin() : b.getFechaHoraFin();

        return new ConflictoAgendaResponse(
                new ResumenReferencia(a.getEspacio().getId(), a.getEspacio().getNombre()),
                mapper.aResponse(a), mapper.aResponse(b), desde, hasta,
                Duration.between(desde, hasta).toMinutes());
    }

    private void validarTipoEditable(TipoActividad tipo) {
        if (tipo.esSincronizado()) {
            throw new BusinessException("Las actividades de tipo " + tipo
                    + " se generan automáticamente desde su módulo; regístrelas en /api/v1/"
                    + (tipo == TipoActividad.ALQUILER ? "alquileres" : "solicitudes-catering"));
        }
    }

    private void aplicar(ActividadAgendaRequest request, ActividadAgenda actividad) {
        IntervaloHorario intervalo = new IntervaloHorario(request.fechaHoraInicio(), request.fechaHoraFin());
        actividad.setTitulo(request.titulo().trim());
        actividad.setTipo(request.tipo());
        actividad.setEspacio(request.espacioId() == null ? null : espacioService.buscar(request.espacioId()));
        actividad.setFechaHoraInicio(intervalo.inicio());
        actividad.setFechaHoraFin(intervalo.fin());
        actividad.setDescripcion(request.descripcion());
        actividad.setResponsable(request.responsable());
    }
}
