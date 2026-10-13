package com.uca.cfc.dto.agenda;

import java.time.LocalDateTime;

import com.uca.cfc.entity.enums.TipoActividad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Las actividades de tipo ALQUILER y CATERING no se crean aquí: las genera el
 * módulo correspondiente para mantener la agenda sincronizada.
 */
public record ActividadAgendaRequest(
        @NotBlank(message = "el título es obligatorio")
        @Size(max = 200, message = "el título no puede exceder 200 caracteres")
        @Schema(example = "Jornada de inducción docente") String titulo,

        @NotNull(message = "el tipo de actividad es obligatorio")
        @Schema(example = "EVENTO") TipoActividad tipo,

        @Positive(message = "el id del espacio debe ser positivo")
        @Schema(description = "Espacio asignado. Se omite en actividades virtuales", example = "2")
        Long espacioId,

        @NotNull(message = "la fecha y hora de inicio es obligatoria")
        @Schema(example = "2026-09-18T08:00:00") LocalDateTime fechaHoraInicio,

        @NotNull(message = "la fecha y hora de fin es obligatoria")
        @Schema(example = "2026-09-18T12:00:00") LocalDateTime fechaHoraFin,

        @Size(max = 500, message = "la descripción no puede exceder 500 caracteres")
        String descripcion,

        @Size(max = 150, message = "el responsable no puede exceder 150 caracteres")
        @Schema(example = "Coordinación académica") String responsable) {
}
