package com.uca.cfc.dto.curso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.entity.enums.TipoModalidad;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Curso tal como se devuelve por la API.
 * Los campos cuposOcupados y cuposDisponibles no estan en la tabla:
 * se calculan contando las inscripciones vigentes.
 */
@Schema(description = "Curso del catálogo académico")
public record CursoResponse(
        Long id,
        @Schema(example = "CUR-014") String codigo,
        String nombre,
        String descripcion,
        ResumenReferencia categoria,
        ResumenReferencia modalidad,
        @Schema(example = "PRESENCIAL") TipoModalidad tipoModalidad,
        ResumenReferencia docente,
        ResumenReferencia espacio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String horario,
        Integer duracionHoras,
        Integer cupoMaximo,
        @Schema(description = "Inscripciones vigentes (no canceladas)", example = "13") Long cuposOcupados,
        @Schema(example = "7") Integer cuposDisponibles,
        BigDecimal precio,
        EstadoOferta estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
