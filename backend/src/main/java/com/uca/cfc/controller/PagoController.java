package com.uca.cfc.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.pago.AbonoPagoRequest;
import com.uca.cfc.dto.pago.CambioEstadoPagoRequest;
import com.uca.cfc.dto.pago.ComprobanteRequest;
import com.uca.cfc.dto.pago.ComprobanteResponse;
import com.uca.cfc.dto.pago.EstadoFinancieroResponse;
import com.uca.cfc.dto.pago.PagoRequest;
import com.uca.cfc.dto.pago.PagoResponse;
import com.uca.cfc.dto.pago.ValidacionPagoRequest;
import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;
import com.uca.cfc.service.PagoService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los pagos. Es el modulo con mas endpoints porque un pago
 * pasa por varios estados: se registra, se le pueden hacer abonos, contabilidad
 * lo valida, recepcion lo confirma y al final se le emite un comprobante.
 * El DELETE no borra el pago de verdad, solo lo marca como eliminado.
 */
@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "Registro, validación y confirmación de pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    @Operation(summary = "Listar pagos",
            description = """
                    Filtros por cliente, concepto, estado, método y rango de fechas.
                    Los pagos con borrado lógico no aparecen en los resultados.""")
    public PageResponse<PagoResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) ConceptoPago concepto,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) MetodoPago metodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fechaPago", direction = Sort.Direction.DESC) Pageable pageable) {
        return pagoService.listar(clienteId, concepto, estado, metodo, desde, hasta, pageable);
    }

    @GetMapping("/reportes/estados-financieros")
    @Operation(summary = "Reporte de estados financieros",
            description = """
                    Totales cobrados y por cobrar en el período, con desglose por estado del
                    pago y por método de pago. Requiere rol CONTABILIDAD o ADMIN a partir de
                    la Fase 3.""")
    public EstadoFinancieroResponse estadosFinancieros(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return pagoService.reporteEstadosFinancieros(desde, hasta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pago")
    public PagoResponse obtener(@PathVariable Long id) {
        return pagoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar un pago",
            description = """
                    El concepto y la referencia deben apuntar a una inscripción, cotización,
                    alquiler o solicitud de catering existente. El estado se deduce del monto
                    recibido: sin monto queda PENDIENTE, con monto parcial PARCIAL y con el
                    total PAGADO.""")
    @ApiResponse(responseCode = "201", description = "Pago registrado")
    @ApiResponse(responseCode = "404", description = "El registro referenciado no existe")
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody PagoRequest request) {
        PagoResponse creado = pagoService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}/abonos")
    @Operation(summary = "Registrar un abono",
            description = "Suma un monto al pago y recalcula su estado. El abono no puede exceder el saldo.")
    public PagoResponse abonar(@PathVariable Long id, @Valid @RequestBody AbonoPagoRequest request) {
        return pagoService.abonar(id, request);
    }

    @PutMapping("/{id}/validar")
    @Operation(summary = "Validar un pago",
            description = "Validación financiera a cargo de CONTABILIDAD. Requiere que el pago tenga monto recibido.")
    @ApiResponse(responseCode = "422", description = "El pago no tiene monto recibido o ya fue validado")
    public PagoResponse validar(@PathVariable Long id, @Valid @RequestBody ValidacionPagoRequest request) {
        return pagoService.validar(id, request.responsable());
    }

    @PutMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar un pago",
            description = "Confirmación en recepción, posterior a la validación de contabilidad.")
    @ApiResponse(responseCode = "422", description = "El pago aún no ha sido validado")
    public PagoResponse confirmar(@PathVariable Long id, @Valid @RequestBody ValidacionPagoRequest request) {
        return pagoService.confirmar(id, request.responsable());
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de un pago",
            description = """
                    El flujo financiero solo avanza: PENDIENTE → PARCIAL → PAGADO.
                    Marcar PAGADO salda el monto del registro.""")
    @ApiResponse(responseCode = "422", description = "La transición de estado no está permitida")
    public PagoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoPagoRequest request) {
        return pagoService.cambiarEstado(id, request.estado());
    }

    @PostMapping("/{id}/comprobante")
    @Operation(summary = "Emitir el comprobante de un pago",
            description = "El número de comprobante se genera con un correlativo anual (CFC-2026-000001).")
    @ApiResponse(responseCode = "201", description = "Comprobante emitido")
    @ApiResponse(responseCode = "409", description = "El pago ya tiene comprobante")
    public ResponseEntity<ComprobanteResponse> emitirComprobante(@PathVariable Long id,
            @RequestBody(required = false) @Valid ComprobanteRequest request) {
        ComprobanteResponse comprobante = pagoService.emitirComprobante(id, request);
        return ResponseEntity.created(UbicacionRecurso.de(comprobante.id())).body(comprobante);
    }

    @GetMapping("/{id}/comprobante")
    @Operation(summary = "Consultar el comprobante de un pago")
    @ApiResponse(responseCode = "404", description = "El pago todavía no tiene comprobante")
    public ComprobanteResponse obtenerComprobante(@PathVariable Long id) {
        return pagoService.obtenerComprobante(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un pago (borrado lógico)",
            description = """
                    El pago se marca como eliminado y deja de aparecer en consultas y
                    reportes, pero permanece en la base de datos para conservar la
                    trazabilidad contable.""")
    @ApiResponse(responseCode = "204", description = "Pago marcado como eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
