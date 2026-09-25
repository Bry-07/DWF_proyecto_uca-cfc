package com.uca.cfc.dto.modalidad;

import com.uca.cfc.entity.enums.TipoModalidad;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Modalidad tal como se devuelve por la API.
 */
@Schema(description = "Modalidad en que se imparte una oferta académica")
public record ModalidadResponse(
        Long id,
        String nombre,
        TipoModalidad tipo,
        String descripcion) {
}
