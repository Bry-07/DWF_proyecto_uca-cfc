package com.uca.cfc.dto.rol;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Rol tal como se devuelve por la API.
 * rolDelSistema indica si es uno de los cuatro roles base, que estan protegidos.
 */
@Schema(description = "Rol de acceso al sistema")
public record RolResponse(
        Long id,
        String nombre,
        String descripcion,
        @Schema(description = "Los roles base del sistema no se pueden renombrar ni eliminar", example = "true")
        boolean rolDelSistema) {
}
