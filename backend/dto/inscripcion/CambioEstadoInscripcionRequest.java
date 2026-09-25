package com.uca.cfc.dto.inscripcion;

import com.uca.cfc.entity.enums.EstadoInscripcion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del endpoint que cambia el estado de una inscripcion.
 * El motivo es opcional y queda guardado en las observaciones.
 */
public record CambioEstadoInscripcionRequest(
        @NotNull(message = "el estado es obligatorio")
        @Schema(example = "CONFIRMADA") EstadoInscripcion estado,

        @Size(max = 500, message = "el motivo no puede exceder 500 caracteres")
        @Schema(example = "Pago confirmado en recepción") String motivo) {
}
