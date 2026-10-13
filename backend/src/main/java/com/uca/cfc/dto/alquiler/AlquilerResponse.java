package com.uca.cfc.dto.alquiler;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoReserva;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Alquiler tal como se devuelve por la API.
 * Incluye las horas que dura la reserva, que es la base del cobro.
 */
@Schema(description = "Alquiler de un espacio del centro")
public record AlquilerResponse(
        Long id,
        @Schema(example = "ALQ-0012") String codigo,
        ResumenReferencia cliente,
        ResumenReferencia espacio,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        @Schema(description = "Duración en horas usada para el cálculo del monto", example = "1.5")
        Double horas,
        String proposito,
        Integer cantidadAsistentes,
        @Schema(example = "67.50") BigDecimal montoTotal,
        EstadoReserva estado,
        String observaciones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
