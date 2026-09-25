package com.uca.cfc.dto.docente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o editar un docente.
 * El patron del telefono obliga al formato 2210-6600.
 */
public record DocenteRequest(
        @NotBlank(message = "los nombres son obligatorios")
        @Size(max = 100, message = "los nombres no pueden exceder 100 caracteres")
        @Schema(example = "Ana Beatriz") String nombres,

        @NotBlank(message = "los apellidos son obligatorios")
        @Size(max = 100, message = "los apellidos no pueden exceder 100 caracteres")
        @Schema(example = "Rivas") String apellidos,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        @Schema(example = "ana.rivas@cfc.uca.edu.sv") String email,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{4}$", message = "el teléfono debe tener el formato 2210-6600")
        @Schema(example = "2210-6600") String telefono,

        @NotBlank(message = "la especialidad es obligatoria")
        @Size(max = 120, message = "la especialidad no puede exceder 120 caracteres")
        @Schema(example = "Gestión de Proyectos") String especialidad) {
}
