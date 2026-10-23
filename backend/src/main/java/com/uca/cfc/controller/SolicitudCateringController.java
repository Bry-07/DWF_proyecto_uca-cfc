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

import com.uca.cfc.dto.alquiler.CambioEstadoReservaRequest;
import com.uca.cfc.dto.catering.SolicitudCateringRequest;
import com.uca.cfc.dto.catering.SolicitudCateringResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.enums.EstadoReserva;
import com.uca.cfc.service.CateringService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de las solicitudes de catering que hacen los clientes.
 * Se diferencia del anterior en que aqui si hay un cliente, una fecha y una
 * cantidad de personas. Si el servicio se da en un espacio del centro, la
 * solicitud tambien aparece en la agenda institucional.
 */
@RestController
@RequestMapping("/api/v1/solicitudes-catering")
@Tag(name = "Solicitudes de catering", description = "Solicitudes de servicio de alimentación")
public class SolicitudCateringController {

    private final CateringService cateringService;

    public SolicitudCateringController(CateringService cateringService) {
        this.cateringService = cateringService;
    }

    @GetMapping
    @Operation(summary = "Listar solicitudes de catering",
            description = "Filtros por cliente, servicio, estado y rango de fechas.")
    public PageResponse<SolicitudCateringResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return cateringService.listarSolicitudes(clienteId, servicioId, estado, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una solicitud de catering")
    public SolicitudCateringResponse obtener(@PathVariable Long id) {
        return cateringService.obtenerSolicitud(id);
    }

    @PostMapping
    @Operation(summary = "Crear una solicitud de catering",
            description = """
                    El monto se calcula multiplicando el precio por persona del servicio por
                    la cantidad de asistentes. Si el servicio se sirve en un espacio del
                    centro, la solicitud aparece también en la agenda institucional.""")
    @ApiResponse(responseCode = "201", description = "Solicitud creada en estado PENDIENTE")
    @ApiResponse(responseCode = "422", description = "El servicio está inactivo o no se alcanza el mínimo de personas")
    public ResponseEntity<SolicitudCateringResponse> crear(
            @Valid @RequestBody SolicitudCateringRequest request) {
        SolicitudCateringResponse creada = cateringService.crearSolicitud(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una solicitud de catering")
    public SolicitudCateringResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody SolicitudCateringRequest request) {
        return cateringService.actualizarSolicitud(id, request);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de una solicitud de catering")
    public SolicitudCateringResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoReservaRequest request) {
        return cateringService.cambiarEstadoSolicitud(id, request.estado(), request.motivo());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una solicitud de catering")
    @ApiResponse(responseCode = "204", description = "Solicitud eliminada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cateringService.eliminarSolicitud(id);
        return ResponseEntity.noContent().build();
    }
}
