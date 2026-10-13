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

import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.dto.espacio.DisponibilidadResponse;
import com.uca.cfc.dto.espacio.EspacioRequest;
import com.uca.cfc.dto.espacio.EspacioResponse;
import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.TipoEspacio;
import com.uca.cfc.service.EspacioService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los espacios fisicos (auditorios, aulas, laboratorios).
 * Lo importante aqui es el endpoint /{id}/disponibilidad, que responde si
 * un espacio esta libre en un rango de fecha y hora, y que franjas ya
 * estan ocupadas. Es el que usaria la pantalla de reservas del portal.
 */
@RestController
@RequestMapping("/api/v1/espacios")
@Tag(name = "Espacios", description = "Auditorios, aulas, salas y laboratorios del centro")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @GetMapping
    @Operation(summary = "Listar espacios", description = "Filtros por tipo, estado, capacidad mínima y nombre.")
    public PageResponse<EspacioResponse> listar(
            @RequestParam(required = false) TipoEspacio tipo,
            @RequestParam(required = false) EstadoEspacio estado,
            @Parameter(description = "Devuelve espacios con capacidad igual o mayor")
            @RequestParam(required = false) Integer capacidadMinima,
            @RequestParam(required = false) String nombre,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return espacioService.listar(tipo, estado, capacidadMinima, nombre, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un espacio")
    public EspacioResponse obtener(@PathVariable Long id) {
        return espacioService.obtener(id);
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "Consultar disponibilidad de un espacio",
            description = """
                    Indica si el espacio está libre en el rango solicitado y devuelve las
                    franjas ocupadas por alquileres vigentes y por actividades de la agenda.
                    Ejemplo: ?desde=2026-09-15T08:00:00&hasta=2026-09-15T18:00:00""")
    public DisponibilidadResponse disponibilidad(@PathVariable Long id,
            @Parameter(description = "Inicio del rango", example = "2026-09-15T08:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fin del rango", example = "2026-09-15T18:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return espacioService.consultarDisponibilidad(id, desde, hasta);
    }

    @PostMapping
    @Operation(summary = "Registrar un espacio")
    @ApiResponse(responseCode = "201", description = "Espacio registrado")
    public ResponseEntity<EspacioResponse> crear(@Valid @RequestBody EspacioRequest request) {
        EspacioResponse creado = espacioService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un espacio")
    public EspacioResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspacioRequest request) {
        return espacioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un espacio",
            description = "No se permite si el espacio tiene alquileres, clases o actividades asociadas.")
    @ApiResponse(responseCode = "204", description = "Espacio eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        espacioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
