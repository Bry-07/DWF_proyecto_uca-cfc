package com.uca.cfc.dto.cliente;

import java.math.BigDecimal;
import java.util.List;

import com.uca.cfc.dto.cotizacion.CotizacionResponse;
import com.uca.cfc.dto.inscripcion.InscripcionResponse;
import com.uca.cfc.dto.pago.PagoResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Historial completo de un cliente.
 * Junta en una sola respuesta sus inscripciones, cotizaciones y pagos,
 * mas un resumen con los totales.
 */
@Schema(description = "Historial completo de un cliente: inscripciones, cotizaciones y pagos")
public record HistorialClienteResponse(
        Long clienteId,
        String cliente,
        Resumen resumen,
        List<InscripcionResponse> inscripciones,
        List<CotizacionResponse> cotizaciones,
        List<PagoResponse> pagos) {

    @Schema(description = "Totales del historial")
    public record Resumen(
            @Schema(example = "6") int totalInscripciones,
            @Schema(example = "3") int totalCotizaciones,
            @Schema(example = "5") int totalPagos,
            @Schema(example = "1250.00") BigDecimal montoPagado) {
    }
}
