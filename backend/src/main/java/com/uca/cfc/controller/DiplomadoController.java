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
import com.uca.cfc.dto.curso.OfertaAcademicaRequest;
import com.uca.cfc.dto.diplomado.DiplomadoResponse;
import com.uca.cfc.entity.enums.EstadoOferta;
import com.uca.cfc.service.DiplomadoService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los diplomados. Funciona igual que el de cursos,
 * porque cursos y diplomados comparten la misma estructura de datos.
 */
@RestController
@RequestMapping("/api/v1/diplomados")
@Tag(name = "Diplomados", description = "Catálogo de diplomados del centro")
public class DiplomadoController {

    private final DiplomadoService diplomadoService;

    public DiplomadoController(DiplomadoService diplomadoService) {
        this.diplomadoService = diplomadoService;
    }

    @GetMapping
    @Operation(summary = "Listar diplomados",
            description = "Mismos filtros y paginación que el catálogo de cursos.")
    public PageResponse<DiplomadoResponse> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long modalidadId,
            @RequestParam(required = false) Long docenteId,
            @RequestParam(required = false) EstadoOferta estado,
            @Parameter(description = "Búsqueda por nombre o código del diplomado")
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "fechaInicio", direction = Sort.Direction.ASC) Pageable pageable) {
        return diplomadoService.listar(categoriaId, modalidadId, docenteId, estado, texto, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un diplomado")
    public DiplomadoResponse obtener(@PathVariable Long id) {
        return diplomadoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un diplomado")
    @ApiResponse(responseCode = "201", description = "Diplomado creado")
    public ResponseEntity<DiplomadoResponse> crear(@Valid @RequestBody OfertaAcademicaRequest request) {
        DiplomadoResponse creado = diplomadoService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un diplomado")
    public DiplomadoResponse actualizar(@PathVariable Long id, @Valid @RequestBody OfertaAcademicaRequest request) {
        return diplomadoService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar un diplomado")
    public DiplomadoResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoOfertaRequest request) {
        return diplomadoService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un diplomado")
    @ApiResponse(responseCode = "204", description = "Diplomado eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        diplomadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
