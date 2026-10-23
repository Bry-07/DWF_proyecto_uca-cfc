package com.uca.cfc.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos del usuario autenticado")
public record PerfilResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        @Schema(description = "Solo si el usuario es un CLIENTE con ficha vinculada") Long clienteId) {
}
