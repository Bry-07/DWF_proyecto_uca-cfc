package com.uca.cfc.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank(message = "el token es obligatorio") String token,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 10, max = 72, message = "la contraseña debe tener entre 10 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "la contraseña debe incluir al menos una minúscula, una mayúscula y un número")
        @Schema(example = "NuevaClave#2026") String nuevaPassword) {
}
