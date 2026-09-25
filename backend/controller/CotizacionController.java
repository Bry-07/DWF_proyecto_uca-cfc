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
import com.uca.cfc.dto.cotizacion.CotizacionRequest;
import com.uca.cfc.dto.cotizacion.CotizacionResponse;
import com.uca.cfc.dto.cotizacion.ResolucionCotizacionRequest;
import com.uca.cfc.entity.enums.EstadoCotizacion;
import com.uca.cfc.entity.enums.TipoCotizacion;
import com.uca.cfc.service.CotizacionService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de las cotizaciones. Una cotizacion se arma con varios items
 * (un curso, un espacio, catering...) y el total se calcula sumandolos.
 * Tiene dos endpoints aparte para aprobarla o rechazarla.
 */
@RestController
@RequestMapping("/api/v1/cotizaciones")
@Tag(name = "Cotizaciones", description = "Cotizaciones de cursos, espacios y catering")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    @Operation(summary = "Listar cotizaciones",
            description = "Filtros por cliente, tipo, estado y rango de fechas de solicitud.")
    public PageResponse<CotizacionResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) TipoCotizacion tipo,
            @RequestParam(required = false) EstadoCotizacion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fechaSolicitud", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return cotizacionService.listar(clienteId, tipo, estado, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una cotización con sus ítems")
    public CotizacionResponse obtener(@PathVariable Long id) {
        return cotizacionService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear una cotización",
            description = """
                    Se construye con ítems de distinto tipo (curso, diplomado, espacio,
                    catering). El total se calcula a partir de los ítems y el tipo se deduce
                    de ellos: con un solo tipo toma ese tipo, con varios es COMBINADA.""")
    @ApiResponse(responseCode = "201", description = "Cotización creada en estado GENERADA")
    public ResponseEntity<CotizacionResponse> crear(@Valid @RequestBody CotizacionRequest request) {
        CotizacionResponse creada = cotizacionService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una cotización",
            description = "Solo mientras esté en estado PENDIENTE o GENERADA.")
    public CotizacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody CotizacionRequest request) {
        return cotizacionService.actualizar(id, request);
    }

    @PutMapping("/{id}/aprobar")
    @Operation(summary = "Aprobar una cotización",
            description = "Requiere rol ADMIN a partir de la Fase 3. Una cotización vencida no puede aprobarse.")
    @ApiResponse(responseCode = "422", description = "La cotización ya fue resuelta o está vencida")
    public CotizacionResponse aprobar(@PathVariable Long id,
            @RequestBody(required = false) @Valid ResolucionCotizacionRequest request) {
        return cotizacionService.aprobar(id, request == null ? null : request.comentario());
    }

    @PutMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una cotización", description = "Requiere rol ADMIN a partir de la Fase 3.")
    public CotizacionResponse rechazar(@PathVariable Long id,
            @RequestBody(required = false) @Valid ResolucionCotizacionRequest request) {
        return cotizacionService.rechazar(id, request == null ? null : request.comentario());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una cotización", description = "No se permite eliminar cotizaciones aprobadas.")
    @ApiResponse(responseCode = "204", description = "Cotización eliminada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cotizacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
