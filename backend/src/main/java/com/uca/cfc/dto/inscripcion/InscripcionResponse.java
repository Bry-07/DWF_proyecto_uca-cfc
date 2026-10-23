package com.uca.cfc.dto.inscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoInscripcion;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Inscripcion tal como se devuelve por la API.
 * Incluye el codigo visible (INS-0182) y si la inscripcion es de un curso
 * o de un diplomado.
 */
@Schema(description = "Inscripción de un participante a un curso o diplomado")
public record InscripcionResponse(
        Long id,
        @Schema(example = "INS-0182") String codigo,
        ResumenReferencia cliente,
        @Schema(example = "CURSO") String tipoOferta,
        ResumenReferencia oferta,
        String participanteNombre,
        String participanteEmail,
        String participanteTelefono,
        LocalDate fechaInscripcion,
        BigDecimal montoTotal,
        EstadoInscripcion estado,
        String observaciones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
