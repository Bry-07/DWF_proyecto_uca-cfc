package com.uca.cfc.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens entregados al iniciar sesión o renovarla")
public record TokenResponse(
        @Schema(description = "JWT para el encabezado Authorization: Bearer") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Segundos que dura el access token", example = "900") long expiresIn,
        @Schema(description = "Se usa en /auth/refresh para pedir un nuevo access token") String refreshToken,
        UsuarioSesion usuario) {

    public record UsuarioSesion(
            @Schema(example = "1") Long id,
            @Schema(example = "Administrador CFC") String nombre,
            @Schema(example = "admin@cfc.uca.edu.sv") String email,
            @Schema(example = "ADMIN") String rol) {
    }
}
