package com.uca.cfc.dto.espacio;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.TipoEspacio;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Espacio tal como se devuelve por la API.
 */
@Schema(description = "Espacio físico del centro")
public record EspacioResponse(
        Long id,
        String nombre,
        TipoEspacio tipo,
        Integer capacidad,
        BigDecimal precioPorHora,
        String equipamiento,
        String ubicacion,
        EstadoEspacio estado) {
}
