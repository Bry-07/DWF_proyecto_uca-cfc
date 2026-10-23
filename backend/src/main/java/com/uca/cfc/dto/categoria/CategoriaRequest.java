package com.uca.cfc.dto.categoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar una categoria.
 * Los Request son los datos que ENTRAN por la API. Las anotaciones
 * (@NotBlank, @Email, @Pattern...) son las validaciones: si algo no cumple,
 * Spring responde 400 antes de que el dato llegue al servicio.
 */
public record CategoriaRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 100, message = "el nombre no puede exceder 100 caracteres")
        @Schema(example = "Gestión de Proyectos") String nombre,

        @Size(max = 255, message = "la descripción no puede exceder 255 caracteres")
        @Schema(example = "Metodologías tradicionales y ágiles de gestión") String descripcion) {
}
