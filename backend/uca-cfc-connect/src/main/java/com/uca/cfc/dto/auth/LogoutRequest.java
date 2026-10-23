package com.uca.cfc.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/** El refresh token es opcional: si no se envía, se cierran todas las sesiones del usuario. */
public record LogoutRequest(
        @Schema(description = "Refresh token de la sesión que se cierra") String refreshToken) {
}
