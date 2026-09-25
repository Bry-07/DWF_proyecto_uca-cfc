package com.uca.cfc.dto.curso;

import com.uca.cfc.entity.enums.EstadoOferta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del endpoint que activa o inactiva un curso o diplomado.
 */
public record CambioEstadoOfertaRequest(
        @NotNull(message = "el estado es obligatorio")
        @Schema(example = "INACTIVO") EstadoOferta estado) {
}
