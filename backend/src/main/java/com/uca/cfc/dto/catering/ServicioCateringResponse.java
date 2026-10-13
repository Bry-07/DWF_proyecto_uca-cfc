package com.uca.cfc.dto.catering;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.TipoServicioCatering;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Servicio de catering tal como se devuelve por la API.
 */
@Schema(description = "Servicio de catering ofrecido por el centro")
public record ServicioCateringResponse(
        Long id,
        @Schema(example = "CAT-0005") String codigo,
        String nombre,
        TipoServicioCatering tipo,
        String descripcion,
        BigDecimal precioPorPersona,
        Integer minimoPersonas,
        boolean activo) {
}
