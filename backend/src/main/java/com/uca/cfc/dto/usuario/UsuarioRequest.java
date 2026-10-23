package com.uca.cfc.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un usuario.
 * La contrasena viaja aqui en texto plano pero se cifra antes de guardarse;
 * en las respuestas nunca se devuelve.
 */
public record UsuarioRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede exceder 150 caracteres")
        @Schema(example = "Rocío Cortez") String nombre,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        @Schema(example = "recepcion@cfc.uca.edu.sv") String email,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 10, max = 72, message = "la contraseña debe tener entre 10 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "la contraseña debe incluir al menos una minúscula, una mayúscula y un número")
        @Schema(example = "Recepcion#2026.Cfc") String password,

        @NotNull(message = "el rol es obligatorio")
        @Positive(message = "el id del rol debe ser positivo")
        @Schema(example = "2") Long rolId) {
}
