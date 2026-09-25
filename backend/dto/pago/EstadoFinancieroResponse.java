package com.uca.cfc.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reporte financiero del centro.
 * Trae el total cobrado, el total por cobrar y el desglose por estado
 * del pago y por metodo de pago.
 */
@Schema(description = "Reporte de estados financieros del centro")
public record EstadoFinancieroResponse(
        LocalDate desde,
        LocalDate hasta,
        @Schema(example = "18420.00") BigDecimal totalCobrado,
        @Schema(example = "2340.00") BigDecimal totalPorCobrar,
        @Schema(example = "86") long cantidadPagos,
        List<ResumenEstado> porEstado,
        List<ResumenMetodo> porMetodo) {

    @Schema(description = "Totales agrupados por estado del pago")
    public record ResumenEstado(
            EstadoPago estado,
            long cantidad,
            BigDecimal montoEsperado,
            BigDecimal montoPagado,
            BigDecimal saldo) {
    }

    @Schema(description = "Totales agrupados por método de pago")
    public record ResumenMetodo(
            MetodoPago metodo,
            long cantidad,
            BigDecimal montoPagado) {
    }
}
