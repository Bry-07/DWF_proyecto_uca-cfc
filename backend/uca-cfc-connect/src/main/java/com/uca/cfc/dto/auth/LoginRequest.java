package com.uca.cfc.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Schema(example = "admin@cfc.uca.edu.sv") String email,

        @NotBlank(message = "la contraseña es obligatoria")
        @Schema(example = "Admin#2026.Cfc") String password) {
}
