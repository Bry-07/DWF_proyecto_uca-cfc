package com.uca.cfc.dto.agenda;

import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Par de actividades que chocan en el mismo espacio.
 * Devuelve las dos actividades, el rango en que se superponen y cuantos
 * minutos dura el traslape.
 */
@Schema(description = "Par de actividades que se superponen en el mismo espacio")
public record ConflictoAgendaResponse(
        ResumenReferencia espacio,
        ActividadAgendaResponse actividadA,
        ActividadAgendaResponse actividadB,
        @Schema(description = "Inicio del traslape", example = "2026-09-17T13:00:00") LocalDateTime desde,
        @Schema(description = "Fin del traslape", example = "2026-09-17T14:00:00") LocalDateTime hasta,
        @Schema(description = "Minutos de traslape", example = "60") long minutosTraslape) {
}
