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
import com.uca.cfc.dto.inscripcion.CambioEstadoInscripcionRequest;
import com.uca.cfc.dto.inscripcion.InscripcionRequest;
import com.uca.cfc.dto.inscripcion.InscripcionResponse;
import com.uca.cfc.dto.inscripcion.InscripcionUpdateRequest;
import com.uca.cfc.entity.enums.EstadoInscripcion;
import com.uca.cfc.service.InscripcionService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de las inscripciones a cursos y diplomados.
 * Aqui se aplica una de las dos reglas criticas del proyecto: al crear una
 * inscripcion el servicio revisa que la oferta todavia tenga cupo. Si ya
 * esta llena responde 409 CONFLICT y no guarda nada.
 */
@RestController
@RequestMapping("/api/v1/inscripciones")
@Tag(name = "Inscripciones", description = "Inscripción de participantes a cursos y diplomados")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @GetMapping
    @Operation(summary = "Listar inscripciones",
            description = "Filtros por cliente, curso, diplomado, estado y rango de fechas de inscripción.")
    public PageResponse<InscripcionResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long diplomadoId,
            @RequestParam(required = false) EstadoInscripcion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fechaInscripcion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return inscripcionService.listar(clienteId, cursoId, diplomadoId, estado, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una inscripción")
    public InscripcionResponse obtener(@PathVariable Long id) {
        return inscripcionService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear una inscripción",
            description = """
                    Indique cursoId o diplomadoId, nunca ambos. Antes de guardar se verifica
                    el cupo máximo de la oferta dentro de la misma transacción: si ya está
                    lleno, la respuesta es 409 CONFLICT y no se registra nada.""")
    @ApiResponse(responseCode = "201", description = "Inscripción creada en estado PENDIENTE")
    @ApiResponse(responseCode = "409", description = "La oferta alcanzó su cupo máximo o el participante ya está inscrito")
    @ApiResponse(responseCode = "422", description = "La oferta está inactiva o no se indicó curso ni diplomado")
    public ResponseEntity<InscripcionResponse> crear(@Valid @RequestBody InscripcionRequest request) {
        InscripcionResponse creada = inscripcionService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar los datos del participante",
            description = """
                    El curso o diplomado no se cambia desde aquí: cancele la inscripción y
                    cree una nueva, de modo que el control de cupos siga siendo consistente.""")
    public InscripcionResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody InscripcionUpdateRequest request) {
        return inscripcionService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de una inscripción",
            description = """
                    Transiciones permitidas: PENDIENTE → CONFIRMADA o CANCELADA;
                    CONFIRMADA → FINALIZADA o CANCELADA. CANCELADA y FINALIZADA son finales.""")
    @ApiResponse(responseCode = "422", description = "La transición de estado no está permitida")
    public InscripcionResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoInscripcionRequest request) {
        return inscripcionService.cambiarEstado(id, request.estado(), request.motivo());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una inscripción")
    @ApiResponse(responseCode = "204", description = "Inscripción eliminada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        inscripcionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
