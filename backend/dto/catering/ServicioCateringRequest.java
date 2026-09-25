package com.uca.cfc.dto.catering;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.TipoServicioCatering;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar un servicio del catalogo de catering.
 */
public record ServicioCateringRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede exceder 120 caracteres")
        @Schema(example = "Coffee break ejecutivo") String nombre,

        @NotNull(message = "el tipo es obligatorio")
        @Schema(example = "COFFEE_BREAK") TipoServicioCatering tipo,

        @Size(max = 500, message = "la descripción no puede exceder 500 caracteres")
        @Schema(example = "Café de especialidad, jugos, fruta y repostería fina") String descripcion,

        @NotNull(message = "el precio por persona es obligatorio")
        @PositiveOrZero(message = "el precio por persona no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "el precio excede el máximo permitido")
        @Schema(example = "8.50") BigDecimal precioPorPersona,

        @NotNull(message = "el mínimo de personas es obligatorio")
        @Positive(message = "el mínimo de personas debe ser mayor que 0")
        @Max(value = 10000, message = "el mínimo de personas excede el máximo permitido")
        @Schema(example = "10") Integer minimoPersonas,

        @Schema(description = "Si se omite, el servicio se crea activo", example = "true") Boolean activo) {
}
