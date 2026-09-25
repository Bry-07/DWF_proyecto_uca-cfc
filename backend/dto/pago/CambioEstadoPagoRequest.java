package com.uca.cfc.dto.pago;

import com.uca.cfc.entity.enums.EstadoPago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del endpoint que cambia el estado de un pago.
 * El flujo solo avanza: PENDIENTE, PARCIAL y despues PAGADO.
 */
public record CambioEstadoPagoRequest(
        @NotNull(message = "el estado es obligatorio")
        @Schema(example = "PAGADO") EstadoPago estado) {
}
