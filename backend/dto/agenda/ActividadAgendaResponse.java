package com.uca.cfc.dto.agenda;

import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.TipoActividad;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Actividad del calendario tal como se devuelve por la API.
 * El campo editable indica si se puede modificar desde la agenda o si hay
 * que hacerlo desde el modulo que la genero (alquileres o catering).
 */
@Schema(description = "Actividad del calendario institucional")
public record ActividadAgendaResponse(
        Long id,
        String titulo,
        TipoActividad tipo,
        ResumenReferencia espacio,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String descripcion,
        String responsable,
        @Schema(description = "Id del alquiler o solicitud de catering que originó la actividad", example = "12")
        Long origenId,
        @Schema(description = "Las actividades sincronizadas se editan desde su módulo de origen",
                example = "false") boolean editable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
