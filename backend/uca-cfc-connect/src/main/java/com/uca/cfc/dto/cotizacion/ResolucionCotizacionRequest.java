package com.uca.cfc.dto.cotizacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Cuerpo opcional de los endpoints de aprobación y rechazo de cotizaciones. */
public record ResolucionCotizacionRequest(
        @Size(max = 500, message = "el comentario no puede exceder 500 caracteres")
        @Schema(example = "Aprobada con disponibilidad confirmada del auditorio") String comentario) {
}
