package com.uca.cfc.dto.modalidad;

import com.uca.cfc.entity.enums.TipoModalidad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar una modalidad.
 */
public record ModalidadRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 50, message = "el nombre no puede exceder 50 caracteres")
        @Schema(example = "Presencial") String nombre,

        @NotNull(message = "el tipo es obligatorio")
        @Schema(example = "PRESENCIAL") TipoModalidad tipo,

        @Size(max = 255, message = "la descripción no puede exceder 255 caracteres")
        String descripcion) {
}
