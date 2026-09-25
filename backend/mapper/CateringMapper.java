package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.catering.ServicioCateringResponse;
import com.uca.cfc.dto.catering.SolicitudCateringResponse;
import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.ServicioCatering;
import com.uca.cfc.entity.SolicitudCatering;
import com.uca.cfc.util.CodigoUtil;

/**
 * Convierte los servicios y las solicitudes de catering en DTOs.
 */
@Component
public class CateringMapper {

    private static final String PREFIJO_SERVICIO = "CAT";
    private static final String PREFIJO_SOLICITUD = "SCA";

    private final ClienteMapper clienteMapper;
    private final CatalogoMapper catalogoMapper;

    public CateringMapper(ClienteMapper clienteMapper, CatalogoMapper catalogoMapper) {
        this.clienteMapper = clienteMapper;
        this.catalogoMapper = catalogoMapper;
    }

    public ServicioCateringResponse aResponse(ServicioCatering servicio) {
        return new ServicioCateringResponse(servicio.getId(),
                CodigoUtil.formatear(PREFIJO_SERVICIO, servicio.getId(), 4), servicio.getNombre(),
                servicio.getTipo(), servicio.getDescripcion(), servicio.getPrecioPorPersona(),
                servicio.getMinimoPersonas(), servicio.isActivo());
    }

    public SolicitudCateringResponse aResponse(SolicitudCatering solicitud) {
        return new SolicitudCateringResponse(solicitud.getId(),
                CodigoUtil.formatear(PREFIJO_SOLICITUD, solicitud.getId(), 4),
                clienteMapper.aResumen(solicitud.getCliente()), aResumen(solicitud.getServicio()),
                catalogoMapper.aResumen(solicitud.getEspacio()), solicitud.getLugar(),
                solicitud.getCantidadAsistentes(), solicitud.getMenu(), solicitud.getFecha(),
                solicitud.getHoraInicio(), solicitud.getHoraFin(), solicitud.getMontoTotal(), solicitud.getEstado(),
                solicitud.getObservaciones(), solicitud.getCreatedAt(), solicitud.getUpdatedAt());
    }

    private ResumenReferencia aResumen(ServicioCatering servicio) {
        return servicio == null ? null : new ResumenReferencia(servicio.getId(), servicio.getNombre());
    }
}
