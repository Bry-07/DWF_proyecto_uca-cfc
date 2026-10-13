package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.agenda.ActividadAgendaResponse;
import com.uca.cfc.entity.ActividadAgenda;

/**
 * Convierte las actividades de la agenda en DTOs.
 * Marca como no editables las que vienen de un alquiler o de una solicitud de
 * catering, porque esas se modifican desde su propio modulo.
 */
@Component
public class AgendaMapper {

    private final CatalogoMapper catalogoMapper;

    public AgendaMapper(CatalogoMapper catalogoMapper) {
        this.catalogoMapper = catalogoMapper;
    }

    public ActividadAgendaResponse aResponse(ActividadAgenda actividad) {
        return new ActividadAgendaResponse(actividad.getId(), actividad.getTitulo(), actividad.getTipo(),
                catalogoMapper.aResumen(actividad.getEspacio()), actividad.getFechaHoraInicio(),
                actividad.getFechaHoraFin(), actividad.getDescripcion(), actividad.getResponsable(),
                actividad.getOrigenId(), !actividad.getTipo().esSincronizado(), actividad.getCreatedAt(),
                actividad.getUpdatedAt());
    }
}
