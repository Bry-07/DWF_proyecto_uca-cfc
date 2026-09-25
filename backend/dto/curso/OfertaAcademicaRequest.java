package com.uca.cfc.dto.curso;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos de creación y actualización de una oferta académica.
 * Cursos y diplomados comparten la misma estructura de campos.
 */
public record OfertaAcademicaRequest(
        @NotBlank(message = "el código es obligatorio")
        @Pattern(regexp = "^[A-Z]{3}-[0-9]{3}$", message = "el código debe tener el formato CUR-014 o DIP-006")
        @Schema(example = "CUR-014") String codigo,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede exceder 150 caracteres")
        @Schema(example = "Gestión Ágil de Proyectos") String nombre,

        @Size(max = 1000, message = "la descripción no puede exceder 1000 caracteres")
        String descripcion,

        @NotNull(message = "la categoría es obligatoria")
        @Positive(message = "el id de la categoría debe ser positivo")
        @Schema(example = "1") Long categoriaId,

        @NotNull(message = "la modalidad es obligatoria")
        @Positive(message = "el id de la modalidad debe ser positivo")
        @Schema(example = "1") Long modalidadId,

        @Positive(message = "el id del docente debe ser positivo")
        @Schema(example = "1") Long docenteId,

        @Positive(message = "el id del espacio debe ser positivo")
        @Schema(description = "Espacio asignado. Se omite en modalidad virtual", example = "3") Long espacioId,

        @NotNull(message = "la fecha de inicio es obligatoria")
        @Schema(example = "2026-09-14") LocalDate fechaInicio,

        @NotNull(message = "la fecha de fin es obligatoria")
        @Schema(example = "2026-10-21") LocalDate fechaFin,

        @Size(max = 100, message = "el horario no puede exceder 100 caracteres")
        @Schema(example = "Lun–Mié · 6:00–8:00 pm") String horario,

        @NotNull(message = "la duración en horas es obligatoria")
        @Positive(message = "la duración debe ser mayor que 0")
        @Max(value = 2000, message = "la duración no puede exceder 2000 horas")
        @Schema(example = "32") Integer duracionHoras,

        @NotNull(message = "el cupo máximo es obligatorio")
        @Positive(message = "el cupo máximo debe ser mayor que 0")
        @Max(value = 1000, message = "el cupo máximo no puede exceder 1000")
        @Schema(example = "20") Integer cupoMaximo,

        @NotNull(message = "el precio es obligatorio")
        @PositiveOrZero(message = "el precio no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "el precio excede el máximo permitido")
        @Schema(example = "185.00") BigDecimal precio) {
}
