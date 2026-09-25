package com.uca.cfc.dto.cliente;

import com.uca.cfc.entity.enums.TipoCliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * La coherencia entre tipo e identificación (DUI para personas, NIT y ficha de
 * empresa para empresas) se valida en el Service, porque depende de varios campos.
 */
public record ClienteRequest(
        @NotNull(message = "el tipo de cliente es obligatorio")
        @Schema(example = "EMPRESA") TipoCliente tipo,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 150, message = "el nombre no puede exceder 150 caracteres")
        @Schema(example = "Laboratorios Vitalab") String nombre,

        @Pattern(regexp = "^$|^[0-9]{8}-[0-9]$", message = "el DUI debe tener el formato 04512378-9")
        @Schema(description = "Obligatorio para PERSONA_PARTICULAR", example = "04512378-9") String dui,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{6}-[0-9]{3}-[0-9]$",
                message = "el NIT debe tener el formato 0614-120589-102-3")
        @Schema(description = "Obligatorio para EMPRESA", example = "0614-120589-102-3") String nit,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        @Schema(example = "contacto@vitalab.com.sv") String email,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{4}$", message = "el teléfono debe tener el formato 2245-8800")
        @Schema(example = "2245-8800") String telefono,

        @Size(max = 255, message = "la dirección no puede exceder 255 caracteres")
        @Schema(example = "Blvd. Los Próceres, San Salvador") String direccion,

        @Valid
        @Schema(description = "Obligatorio para EMPRESA, se ignora para PERSONA_PARTICULAR")
        EmpresaRequest empresa) {
}
