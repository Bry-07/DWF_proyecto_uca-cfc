package com.uca.cfc.dto.inscripcion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Actualización de una inscripción. El curso o diplomado no se cambia: para
 * eso se cancela la inscripción y se crea una nueva, de modo que el control de
 * cupos permanezca consistente.
 */
public record InscripcionUpdateRequest(
        @NotBlank(message = "el nombre del participante es obligatorio")
        @Size(max = 150, message = "el nombre del participante no puede exceder 150 caracteres")
        @Schema(example = "Marta Elena Gómez") String participanteNombre,

        @NotBlank(message = "el correo del participante es obligatorio")
        @Email(message = "el correo del participante no tiene un formato válido")
        @Size(max = 150, message = "el correo no puede exceder 150 caracteres")
        @Schema(example = "marta.gomez@vitalab.com.sv") String participanteEmail,

        @Pattern(regexp = "^$|^[0-9]{4}-[0-9]{4}$", message = "el teléfono debe tener el formato 7890-1122")
        String participanteTelefono,

        @Size(max = 500, message = "las observaciones no pueden exceder 500 caracteres")
        String observaciones) {
}
