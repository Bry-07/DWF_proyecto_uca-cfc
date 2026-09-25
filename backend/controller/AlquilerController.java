package com.uca.cfc.controller;

import java.time.LocalDateTime;

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

import com.uca.cfc.dto.alquiler.AlquilerRequest;
import com.uca.cfc.dto.alquiler.AlquilerResponse;
import com.uca.cfc.dto.alquiler.CambioEstadoReservaRequest;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.enums.EstadoReserva;
import com.uca.cfc.service.AlquilerService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador del alquiler de espacios.
 * Aqui esta la otra regla critica: antes de guardar una reserva se revisa que
 * no haya otra actividad en ese mismo espacio a esa misma hora. Si se
 * superponen responde 409 CONFLICT.
 */
@RestController
@RequestMapping("/api/v1/alquileres")
@Tag(name = "Alquileres", description = "Reserva de espacios del centro")
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @GetMapping
    @Operation(summary = "Listar alquileres",
            description = "Filtros por cliente, espacio, estado y rango de fecha y hora.")
    public PageResponse<AlquilerResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long espacioId,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 10, sort = "fechaHoraInicio", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return alquilerService.listar(clienteId, espacioId, estado, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un alquiler")
    public AlquilerResponse obtener(@PathVariable Long id) {
        return alquilerService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un alquiler",
            description = """
                    Antes de guardar se verifica que ninguna actividad del mismo espacio se
                    superponga con el horario solicitado (nuevoInicio < existenteFin y
                    nuevoFin > existenteInicio). Si hay traslape se devuelve 409 CONFLICT.
                    El monto se calcula por hora según el precio del espacio, y la reserva
                    se refleja automáticamente en la agenda institucional.""")
    @ApiResponse(responseCode = "201", description = "Alquiler creado en estado PENDIENTE")
    @ApiResponse(responseCode = "409", description = "El espacio ya está ocupado en ese horario")
    @ApiResponse(responseCode = "422", description = "El espacio no está disponible o su capacidad es insuficiente")
    public ResponseEntity<AlquilerResponse> crear(@Valid @RequestBody AlquilerRequest request) {
        AlquilerResponse creado = alquilerService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un alquiler",
            description = "Se vuelve a verificar el traslape, ignorando la propia reserva.")
    @ApiResponse(responseCode = "409", description = "El espacio ya está ocupado en el nuevo horario")
    public AlquilerResponse actualizar(@PathVariable Long id, @Valid @RequestBody AlquilerRequest request) {
        return alquilerService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de un alquiler",
            description = """
                    Transiciones permitidas: PENDIENTE → CONFIRMADA o CANCELADA;
                    CONFIRMADA → FINALIZADA o CANCELADA. Al cancelar, el espacio queda
                    liberado y la actividad se retira de la agenda.""")
    public AlquilerResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoReservaRequest request) {
        return alquilerService.cambiarEstado(id, request.estado(), request.motivo());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un alquiler")
    @ApiResponse(responseCode = "204", description = "Alquiler eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        alquilerService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
