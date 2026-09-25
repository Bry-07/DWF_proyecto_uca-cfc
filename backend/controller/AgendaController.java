package com.uca.cfc.controller;

import java.time.LocalDateTime;
import java.util.List;

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

import com.uca.cfc.dto.agenda.ActividadAgendaRequest;
import com.uca.cfc.dto.agenda.ActividadAgendaResponse;
import com.uca.cfc.dto.agenda.ConflictoAgendaResponse;
import com.uca.cfc.dto.common.PageResponse;
import com.uca.cfc.entity.enums.TipoActividad;
import com.uca.cfc.service.AgendaService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de la agenda institucional, que es el calendario unico del centro.
 * Junta cursos, diplomados, alquileres, catering y eventos.
 * El endpoint /conflictos revisa si hay dos actividades en el mismo espacio
 * a la misma hora.
 */
@RestController
@RequestMapping("/api/v1/agenda")
@Tag(name = "Agenda institucional",
        description = "Calendario centralizado de cursos, diplomados, alquileres, catering y eventos")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping
    @Operation(summary = "Consultar la agenda",
            description = "Filtros por tipo de actividad, espacio y rango de fechas (fechaDesde / fechaHasta).")
    public PageResponse<ActividadAgendaResponse> listar(
            @RequestParam(required = false) TipoActividad tipo,
            @RequestParam(required = false) Long espacioId,
            @Parameter(example = "2026-09-14T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaDesde,
            @Parameter(example = "2026-09-20T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaHasta,
            @PageableDefault(size = 20, sort = "fechaHoraInicio", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return agendaService.listar(tipo, espacioId, fechaDesde, fechaHasta, pageable);
    }

    @GetMapping("/conflictos")
    @Operation(summary = "Detectar conflictos de agenda",
            description = """
                    Devuelve los pares de actividades que ocupan el mismo espacio en horarios
                    que se superponen, con el rango y los minutos de traslape. Es la consulta
                    que alimenta el indicador de conflictos del calendario.""")
    public List<ConflictoAgendaResponse> conflictos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaHasta) {
        return agendaService.detectarConflictos(fechaDesde, fechaHasta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una actividad de la agenda")
    public ActividadAgendaResponse obtener(@PathVariable Long id) {
        return agendaService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar una actividad",
            description = """
                    Para actividades propias del centro: cursos, diplomados y eventos. Las de
                    tipo ALQUILER y CATERING se generan automáticamente desde sus módulos.""")
    @ApiResponse(responseCode = "201", description = "Actividad registrada")
    @ApiResponse(responseCode = "422", description = "El tipo de actividad se gestiona desde otro módulo")
    public ResponseEntity<ActividadAgendaResponse> crear(@Valid @RequestBody ActividadAgendaRequest request) {
        ActividadAgendaResponse creada = agendaService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una actividad de la agenda")
    public ActividadAgendaResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody ActividadAgendaRequest request) {
        return agendaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una actividad de la agenda")
    @ApiResponse(responseCode = "204", description = "Actividad eliminada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        agendaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
