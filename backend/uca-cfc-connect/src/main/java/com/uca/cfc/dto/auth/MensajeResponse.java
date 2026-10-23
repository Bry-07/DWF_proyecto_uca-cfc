package com.uca.cfc.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mensaje de confirmación")
public record MensajeResponse(
        String mensaje,
        @Schema(description = "Solo en desarrollo (app.seguridad.mostrar-token-recuperacion=true), "
                + "para probar la recuperación sin servidor de correo") String tokenDePrueba) {
}
