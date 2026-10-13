package com.uca.cfc.dto.alquiler;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para reservar un espacio.
 * El monto no se manda: lo calcula el servicio segun las horas reservadas
 * y el precio por hora del espacio.
 */
public record AlquilerRequest(
        @NotNull(message = "el cliente es obligatorio")
        @Positive(message = "el id del cliente debe ser positivo")
        @Schema(example = "2") Long clienteId,

        @NotNull(message = "el espacio es obligatorio")
        @Positive(message = "el id del espacio debe ser positivo")
        @Schema(example = "1") Long espacioId,

        @NotNull(message = "la fecha y hora de inicio es obligatoria")
        @Schema(example = "2026-09-15T17:00:00") LocalDateTime fechaHoraInicio,

        @NotNull(message = "la fecha y hora de fin es obligatoria")
        @Schema(example = "2026-09-15T18:30:00") LocalDateTime fechaHoraFin,

        @NotBlank(message = "el propósito es obligatorio")
        @Size(max = 255, message = "el propósito no puede exceder 255 caracteres")
        @Schema(example = "Asamblea de accionistas") String proposito,

        @NotNull(message = "la cantidad de asistentes es obligatoria")
        @Positive(message = "la cantidad de asistentes debe ser mayor que 0")
        @Max(value = 10000, message = "la cantidad de asistentes excede el máximo permitido")
        @Schema(example = "120") Integer cantidadAsistentes,

        @Size(max = 500, message = "las observaciones no pueden exceder 500 caracteres")
        String observaciones) {
}
