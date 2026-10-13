package com.uca.cfc.dto.cotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoCotizacion;
import com.uca.cfc.entity.enums.TipoCotizacion;
import com.uca.cfc.entity.enums.TipoItemCotizacion;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cotizacion tal como se devuelve por la API, junto con todos sus items.
 */
@Schema(description = "Cotización de servicios del centro")
public record CotizacionResponse(
        Long id,
        @Schema(example = "COT-0032") String codigo,
        ResumenReferencia cliente,
        TipoCotizacion tipo,
        EstadoCotizacion estado,
        String asunto,
        LocalDate fechaSolicitud,
        LocalDate fechaVencimiento,
        @Schema(example = "5017.50") BigDecimal total,
        String notaInterna,
        String responsableRevision,
        List<ItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    @Schema(description = "Línea de la cotización")
    public record ItemResponse(
            Long id,
            TipoItemCotizacion tipo,
            String concepto,
            String detalle,
            Long referenciaId,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }
}
