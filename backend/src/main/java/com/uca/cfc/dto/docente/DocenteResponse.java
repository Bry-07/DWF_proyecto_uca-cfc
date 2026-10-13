package com.uca.cfc.dto.docente;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Docente tal como se devuelve por la API.
 * Incluye el nombre completo ya armado, para que el front no tenga que unirlo.
 */
@Schema(description = "Docente del centro")
public record DocenteResponse(
        Long id,
        String nombres,
        String apellidos,
        @Schema(example = "Ana Beatriz Rivas") String nombreCompleto,
        String email,
        String telefono,
        String especialidad) {
}
