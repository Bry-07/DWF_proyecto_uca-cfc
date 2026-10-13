package com.uca.cfc.dto.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos adicionales obligatorios cuando el cliente es de tipo EMPRESA. */
public record EmpresaRequest(
        @NotBlank(message = "la razón social es obligatoria")
        @Size(max = 200, message = "la razón social no puede exceder 200 caracteres")
        @Schema(example = "Laboratorios Vitalab, S.A. de C.V.") String razonSocial,

        @Size(max = 150, message = "el giro no puede exceder 150 caracteres")
        @Schema(example = "Industria farmacéutica") String giro,

        @Size(max = 150, message = "el nombre de contacto no puede exceder 150 caracteres")
        @Schema(example = "Marta Elena Gómez") String contactoNombre,

        @Size(max = 100, message = "el cargo de contacto no puede exceder 100 caracteres")
        @Schema(example = "Jefa de Talento Humano") String contactoCargo) {
}
