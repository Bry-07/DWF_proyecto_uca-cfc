package com.uca.cfc.dto.usuario;

import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoUsuario;

import io.swagger.v3.oas.annotations.media.Schema;

/** La respuesta nunca incluye la contraseña ni su hash. */
@Schema(description = "Usuario del sistema")
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        EstadoUsuario estado,
        ResumenReferencia rol,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
