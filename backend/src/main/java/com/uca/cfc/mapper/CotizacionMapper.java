package com.uca.cfc.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.cotizacion.CotizacionResponse;
import com.uca.cfc.entity.Cotizacion;
import com.uca.cfc.entity.CotizacionItem;
import com.uca.cfc.util.CodigoUtil;

/**
 * Convierte las cotizaciones y sus items en DTOs.
 */
@Component
public class CotizacionMapper {

    private static final String PREFIJO = "COT";

    private final ClienteMapper clienteMapper;

    public CotizacionMapper(ClienteMapper clienteMapper) {
        this.clienteMapper = clienteMapper;
    }

    public CotizacionResponse aResponse(Cotizacion cotizacion) {
        List<CotizacionResponse.ItemResponse> items = cotizacion.getItems().stream().map(this::aItemResponse).toList();

        return new CotizacionResponse(cotizacion.getId(), CodigoUtil.formatear(PREFIJO, cotizacion.getId(), 4),
                clienteMapper.aResumen(cotizacion.getCliente()), cotizacion.getTipo(), cotizacion.getEstado(),
                cotizacion.getAsunto(), cotizacion.getFechaSolicitud(), cotizacion.getFechaVencimiento(),
                cotizacion.getTotal(), cotizacion.getNotaInterna(), cotizacion.getResponsableRevision(), items,
                cotizacion.getCreatedAt(), cotizacion.getUpdatedAt());
    }

    private CotizacionResponse.ItemResponse aItemResponse(CotizacionItem item) {
        return new CotizacionResponse.ItemResponse(item.getId(), item.getTipo(), item.getConcepto(),
                item.getDetalle(), item.getReferenciaId(), item.getCantidad(), item.getPrecioUnitario(),
                item.getSubtotal());
    }
}
