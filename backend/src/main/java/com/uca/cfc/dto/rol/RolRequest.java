package com.uca.cfc.dto.rol;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar un rol.
 * El patron obliga a escribir el nombre en mayusculas.
 */
public record RolRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 50, message = "el nombre no puede exceder 50 caracteres")
        @Pattern(regexp = "^[A-Z][A-Z_]*$", message = "el nombre debe escribirse en mayúsculas, por ejemplo COORDINADOR")
        @Schema(example = "COORDINADOR") String nombre,

        @Size(max = 255, message = "la descripción no puede exceder 255 caracteres")
        @Schema(example = "Coordina la programación académica") String descripcion) {
}
