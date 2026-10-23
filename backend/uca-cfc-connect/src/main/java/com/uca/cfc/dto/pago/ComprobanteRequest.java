package com.uca.cfc.dto.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** El número, la fecha, el monto y el destinatario los genera el sistema. */
public record ComprobanteRequest(
        @Size(max = 500, message = "las observaciones no pueden exceder 500 caracteres")
        @Schema(example = "Inscripción CUR-014 · Marta Elena Gómez") String observaciones) {
}
