package com.uca.cfc.dto.usuario;

import com.uca.cfc.entity.enums.EstadoUsuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del endpoint que activa o inactiva un usuario.
 */
public record CambioEstadoUsuarioRequest(
        @NotNull(message = "el estado es obligatorio")
        @Schema(example = "INACTIVO") EstadoUsuario estado) {
}
