package com.uca.cfc.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uca.cfc.entity.enums.MetodoPago;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Comprobante de pago tal como se devuelve por la API.
 */
@Schema(description = "Comprobante de un pago")
public record ComprobanteResponse(
        Long id,
        @Schema(example = "CFC-2026-000001") String numero,
        Long pagoId,
        @Schema(example = "PAG-0231") String codigoPago,
        String emitidoA,
        BigDecimal monto,
        MetodoPago metodo,
        LocalDateTime fechaEmision,
        String observaciones) {
}
