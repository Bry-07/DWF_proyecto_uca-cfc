package com.uca.cfc.dto.diplomado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.entity.enums.TipoModalidad;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Diplomado tal como se devuelve por la API.
 */
@Schema(description = "Diplomado del catálogo académico")
public record DiplomadoResponse(
        Long id,
        @Schema(example = "DIP-006") String codigo,
        String nombre,
        String descripcion,
        ResumenReferencia categoria,
        ResumenReferencia modalidad,
        @Schema(example = "VIRTUAL") TipoModalidad tipoModalidad,
        ResumenReferencia docente,
        ResumenReferencia espacio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String horario,
        Integer duracionHoras,
        Integer cupoMaximo,
        @Schema(example = "27") Long cuposOcupados,
        @Schema(example = "3") Integer cuposDisponibles,
        BigDecimal precio,
        EstadoOferta estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
