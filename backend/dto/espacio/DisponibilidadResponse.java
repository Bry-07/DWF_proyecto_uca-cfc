package com.uca.cfc.dto.espacio;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de la consulta de disponibilidad de un espacio.
 * Dice si esta libre en el rango pedido y, si no lo esta, que franjas
 * estan ocupadas y por que actividad.
 */
@Schema(description = "Disponibilidad de un espacio en un rango de fecha y hora")
public record DisponibilidadResponse(
        Long espacioId,
        String espacio,
        LocalDateTime desde,
        LocalDateTime hasta,
        @Schema(description = "Falso si existe al menos una ocupación en el rango", example = "true")
        boolean disponible,
        @Schema(description = "Actividades que ocupan el espacio dentro del rango consultado")
        List<Ocupacion> ocupaciones) {

    @Schema(description = "Franja ocupada del espacio")
    public record Ocupacion(
            @Schema(example = "ALQUILER") String origen,
            @Schema(example = "12") Long referenciaId,
            @Schema(example = "Asamblea de accionistas") String descripcion,
            LocalDateTime inicio,
            LocalDateTime fin) {
    }
}
