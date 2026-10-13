package com.uca.cfc.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

/** Referencia breve a otra entidad dentro de una respuesta. */
@Schema(description = "Referencia resumida a otro recurso")
public record ResumenReferencia(
        @Schema(example = "3") Long id,
        @Schema(example = "Gestión Ágil de Proyectos") String nombre) {
}
