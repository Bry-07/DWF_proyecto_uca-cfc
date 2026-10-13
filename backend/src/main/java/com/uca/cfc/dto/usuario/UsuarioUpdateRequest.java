package com.uca.cfc.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** La contraseña es opcional: si viene vacía, se conserva la actual. */
public record UsuarioUpdateRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede exceder 150 caracteres")
        String nombre,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        String email,

        @Size(min = 10, max = 72, message = "la contraseña debe tener entre 10 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "la contraseña debe incluir al menos una minúscula, una mayúscula y un número")
        @Schema(description = "Solo si se desea cambiar la contraseña") String password,

        @NotNull(message = "el rol es obligatorio")
        @Positive(message = "el id del rol debe ser positivo")
        Long rolId) {
}
