package com.uca.cfc.dto.alquiler;

import com.uca.cfc.entity.enums.EstadoReserva;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambio de estado de un alquiler o de una solicitud de catering. */
public record CambioEstadoReservaRequest(
        @NotNull(message = "el estado es obligatorio")
        @Schema(example = "CONFIRMADA") EstadoReserva estado,

        @Size(max = 500, message = "el motivo no puede exceder 500 caracteres")
        String motivo) {
}
