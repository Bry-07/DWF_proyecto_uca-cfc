package com.uca.cfc.dto.catering;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * El lugar puede indicarse con espacioId (espacio del centro) o con el texto
 * libre lugar. Debe venir al menos uno de los dos.
 */
public record SolicitudCateringRequest(
        @NotNull(message = "el cliente es obligatorio")
        @Positive(message = "el id del cliente debe ser positivo")
        @Schema(example = "3") Long clienteId,

        @NotNull(message = "el servicio de catering es obligatorio")
        @Positive(message = "el id del servicio debe ser positivo")
        @Schema(example = "2") Long servicioId,

        @Positive(message = "el id del espacio debe ser positivo")
        @Schema(description = "Espacio del centro donde se sirve, si aplica", example = "9") Long espacioId,

        @Size(max = 255, message = "el lugar no puede exceder 255 caracteres")
        @Schema(description = "Obligatorio si no se indica un espacio del centro",
                example = "Sala Multimedia — Edificio Norte") String lugar,

        @NotNull(message = "la cantidad de asistentes es obligatoria")
        @Positive(message = "la cantidad de asistentes debe ser mayor que 0")
        @Max(value = 10000, message = "la cantidad de asistentes excede el máximo permitido")
        @Schema(example = "30") Integer cantidadAsistentes,

        @Size(max = 1000, message = "el menú no puede exceder 1000 caracteres")
        @Schema(example = "Café de especialidad, jugos naturales, fruta de temporada y repostería") String menu,

        @NotNull(message = "la fecha es obligatoria")
        @Schema(example = "2026-09-17") LocalDate fecha,

        @NotNull(message = "la hora de inicio es obligatoria")
        @Schema(example = "11:30:00") LocalTime horaInicio,

        @NotNull(message = "la hora de fin es obligatoria")
        @Schema(example = "12:15:00") LocalTime horaFin,

        @Size(max = 500, message = "las observaciones no pueden exceder 500 caracteres")
        String observaciones) {
}
