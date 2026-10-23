package com.uca.cfc.dto.inscripcion;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Debe indicarse exactamente un cursoId o un diplomadoId; el Service rechaza
 * la solicitud si vienen ambos o ninguno.
 */
public record InscripcionRequest(
        @NotNull(message = "el cliente es obligatorio")
        @Positive(message = "el id del cliente debe ser positivo")
        @Schema(example = "1") Long clienteId,

        @Positive(message = "el id del curso debe ser positivo")
        @Schema(description = "Indique cursoId o diplomadoId, no ambos", example = "2") Long cursoId,

        @Positive(message = "el id del diplomado debe ser positivo")
        @Schema(example = "null") Long diplomadoId,

        @NotBlank(message = "el nombre del participante es obligatorio")
        @Size(max = 150, message = "el nombre del participante no puede exceder 150 caracteres")
        @Schema(example = "Marta Elena Gómez") String participanteNombre,

        @NotBlank(message = "el correo del participante es obligatorio")
        @Email(message = "el correo del participante no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        @Schema(example = "marta.gomez@vitalab.com.sv") String participanteEmail,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{4}$", message = "el teléfono debe tener el formato 7890-1122")
        @Schema(example = "7890-1122") String participanteTelefono,

        @Schema(description = "Si se omite, se usa la fecha actual", example = "2026-09-11")
        LocalDate fechaInscripcion,

        @Size(max = 500, message = "las observaciones no pueden exceder 500 caracteres")
        String observaciones) {
}
