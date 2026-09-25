package com.uca.cfc.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pago tal como se devuelve por la API.
 * Incluye el saldo pendiente y quien valido y confirmo el pago.
 */
@Schema(description = "Pago registrado en el sistema")
public record PagoResponse(
        Long id,
        @Schema(example = "PAG-0231") String codigo,
        ResumenReferencia cliente,
        ConceptoPago concepto,
        Long referenciaId,
        BigDecimal montoEsperado,
        BigDecimal montoPagado,
        @Schema(example = "0.00") BigDecimal saldo,
        MetodoPago metodo,
        EstadoPago estado,
        LocalDate fechaPago,
        String referenciaBancaria,
        LocalDateTime fechaValidacion,
        String validadoPor,
        LocalDateTime fechaConfirmacion,
        String confirmadoPor,
        @Schema(example = "true") boolean tieneComprobante,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
