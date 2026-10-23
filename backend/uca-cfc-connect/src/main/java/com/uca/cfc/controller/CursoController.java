package com.uca.cfc.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import com.uca.cfc.dto.curso.CambioEstadoOfertaRequest;
import com.uca.cfc.dto.curso.CursoResponse;
import com.uca.cfc.dto.curso.OfertaAcademicaRequest;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.service.CursoService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador del catalogo de cursos. Es uno de los mas usados del sistema.
 * Ademas del CRUD tiene el endpoint /{id}/estado para activar o inactivar
 * un curso sin borrarlo, porque un curso con inscripciones no se puede eliminar.
 */
@RestController
@RequestMapping("/api/v1/cursos")
@Tag(name = "Cursos", description = "Catálogo de cursos del centro")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    @Operation(summary = "Listar cursos",
            description = """
                    Filtros disponibles: categoría, modalidad, docente, estado y texto libre
                    (nombre o código). Admite paginación (?page=0&size=10) y ordenamiento
                    (?sort=fechaInicio,desc). Cada curso incluye sus cupos ocupados y disponibles.""")
    public PageResponse<CursoResponse> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long modalidadId,
            @RequestParam(required = false) Long docenteId,
            @RequestParam(required = false) EstadoOferta estado,
            @Parameter(description = "Búsqueda por nombre o código del curso")
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "fechaInicio", direction = Sort.Direction.ASC) Pageable pageable) {
        return cursoService.listar(categoriaId, modalidadId, docenteId, estado, texto, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un curso")
    public CursoResponse obtener(@PathVariable Long id) {
        return cursoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un curso", description = "Requiere rol ADMIN a partir de la Fase 3.")
    @ApiResponse(responseCode = "201", description = "Curso creado")
    @ApiResponse(responseCode = "409", description = "Ya existe un curso con ese código")
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody OfertaAcademicaRequest request) {
        CursoResponse creado = cursoService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un curso",
            description = "El cupo máximo no puede quedar por debajo de las inscripciones vigentes.")
    public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody OfertaAcademicaRequest request) {
        return cursoService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar un curso",
            description = "Un curso INACTIVO permanece en el catálogo pero no admite nuevas inscripciones.")
    public CursoResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoOfertaRequest request) {
        return cursoService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un curso",
            description = "Solo es posible si el curso no tiene inscripciones registradas.")
    @ApiResponse(responseCode = "204", description = "Curso eliminado")
    @ApiResponse(responseCode = "409", description = "El curso tiene inscripciones")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
