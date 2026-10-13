package com.uca.cfc.dto.espacio;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.TipoEspacio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o editar un espacio.
 */
public record EspacioRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 100, message = "el nombre no puede exceder 100 caracteres")
        @Schema(example = "Auditorio Principal") String nombre,

        @NotNull(message = "el tipo es obligatorio")
        @Schema(example = "AUDITORIO") TipoEspacio tipo,

        @NotNull(message = "la capacidad es obligatoria")
        @Positive(message = "la capacidad debe ser mayor que 0")
        @Max(value = 10000, message = "la capacidad no puede exceder 10000")
        @Schema(example = "180") Integer capacidad,

        @NotNull(message = "el precio por hora es obligatorio")
        @PositiveOrZero(message = "el precio por hora no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "el precio excede el máximo permitido")
        @Schema(example = "45.00") BigDecimal precioPorHora,

        @Size(max = 500, message = "el equipamiento no puede exceder 500 caracteres")
        @Schema(example = "Proyector 4K, sonido profesional, tarima") String equipamiento,

        @Size(max = 150, message = "la ubicación no puede exceder 150 caracteres")
        @Schema(example = "Edificio Central, planta baja") String ubicacion,

        @Schema(description = "Si se omite, el espacio se crea como DISPONIBLE", example = "DISPONIBLE")
        EstadoEspacio estado) {
}
