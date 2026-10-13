package com.uca.cfc.dto.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Responsable de la validación (CONTABILIDAD) o de la confirmación (RECEPCIONISTA).
 * Cuando exista autenticación, este dato se tomará del usuario en sesión.
 */
public record ValidacionPagoRequest(
        @NotBlank(message = "el responsable es obligatorio")
        @Size(max = 100, message = "el responsable no puede exceder 100 caracteres")
        @Schema(example = "contabilidad@cfc.uca.edu.sv") String responsable) {
}
