package com.uca.cfc.dto.categoria;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Categoria tal como se devuelve por la API.
 * Los Response son los datos que SALEN por la API. Se usa un record aparte
 * de la entidad para controlar exactamente que campos se exponen.
 */
@Schema(description = "Categoría del catálogo académico")
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
