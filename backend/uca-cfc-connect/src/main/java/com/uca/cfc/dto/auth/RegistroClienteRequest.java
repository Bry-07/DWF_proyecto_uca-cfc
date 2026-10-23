package com.uca.cfc.dto.auth;

import com.uca.cfc.dto.cliente.EmpresaRequest;
import com.uca.cfc.entity.enums.TipoCliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Autorregistro de un cliente: crea su cuenta de acceso y su ficha de cliente en un solo paso.
 * El rol siempre es CLIENTE, no se puede elegir.
 */
public record RegistroClienteRequest(
        @NotNull(message = "el tipo de cliente es obligatorio")
        @Schema(example = "PERSONA_PARTICULAR") TipoCliente tipo,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede exceder 150 caracteres")
        @Schema(example = "Carlos Menjívar") String nombre,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        @Schema(example = "carlos.menjivar@correo.com") String email,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 10, max = 72, message = "la contraseña debe tener entre 10 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "la contraseña debe incluir al menos una minúscula, una mayúscula y un número")
        @Schema(example = "MiClave#2026") String password,

        @Pattern(regexp = "^$|^[0-9]{8}-[0-9]$", message = "el DUI debe tener el formato 04512378-9")
        @Schema(description = "Obligatorio para PERSONA_PARTICULAR", example = "04512378-9") String dui,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{6}-[0-9]{3}-[0-9]$",
                message = "el NIT debe tener el formato 0614-120589-102-3")
        @Schema(description = "Obligatorio para EMPRESA", example = "0614-120589-102-3") String nit,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{4}$", message = "el teléfono debe tener el formato 2245-8800")
        @Schema(example = "7788-1122") String telefono,

        @Size(max = 255, message = "la dirección no puede exceder 255 caracteres")
        String direccion,

        @Valid
        @Schema(description = "Obligatorio para EMPRESA") EmpresaRequest empresa) {
}
