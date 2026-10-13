package com.uca.cfc.dto.cotizacion;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.TipoItemCotizacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Una linea de la cotizacion: que se cotiza, cuantos y a que precio.
 * El subtotal no se manda, lo calcula el sistema.
 */
public record CotizacionItemRequest(
        @NotNull(message = "el tipo del ítem es obligatorio")
        @Schema(example = "CURSO") TipoItemCotizacion tipo,

        @NotBlank(message = "el concepto es obligatorio")
        @Size(max = 255, message = "el concepto no puede exceder 255 caracteres")
        @Schema(example = "Curso empresarial") String concepto,

        @Size(max = 255, message = "el detalle no puede exceder 255 caracteres")
        @Schema(example = "CUR-014 · 25 participantes") String detalle,

        @Positive(message = "el id de referencia debe ser positivo")
        @Schema(description = "Id del curso, diplomado, espacio o servicio cotizado", example = "2")
        Long referenciaId,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad debe ser mayor que 0")
        @Max(value = 100000, message = "la cantidad excede el máximo permitido")
        @Schema(example = "25") Integer cantidad,

        @NotNull(message = "el precio unitario es obligatorio")
        @PositiveOrZero(message = "el precio unitario no puede ser negativo")
        @DecimalMax(value = "99999999.99", message = "el precio unitario excede el máximo permitido")
        @Schema(example = "185.00") BigDecimal precioUnitario) {
}
