package com.uca.cfc.dto.catering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.EstadoReserva;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Solicitud de catering tal como se devuelve por la API.
 */
@Schema(description = "Solicitud de un servicio de catering")
public record SolicitudCateringResponse(
        Long id,
        @Schema(example = "SCA-0007") String codigo,
        ResumenReferencia cliente,
        ResumenReferencia servicio,
        ResumenReferencia espacio,
        String lugar,
        Integer cantidadAsistentes,
        String menu,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        @Schema(example = "255.00") BigDecimal montoTotal,
        EstadoReserva estado,
        String observaciones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
