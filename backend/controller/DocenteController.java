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
import com.uca.cfc.dto.docente.DocenteRequest;
import com.uca.cfc.dto.docente.DocenteResponse;
import com.uca.cfc.service.DocenteService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los docentes del centro.
 * Permite buscarlos por especialidad o por nombre, ademas del CRUD normal.
 */
@RestController
@RequestMapping("/api/v1/docentes")
@Tag(name = "Docentes", description = "Docentes que imparten cursos y diplomados")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping
    @Operation(summary = "Listar docentes", description = "Admite filtro por especialidad y por nombre.")
    public PageResponse<DocenteResponse> listar(
            @Parameter(description = "Coincidencia parcial de la especialidad")
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String nombre,
            @PageableDefault(size = 10, sort = "apellidos", direction = Sort.Direction.ASC) Pageable pageable) {
        return docenteService.listar(especialidad, nombre, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un docente")
    public DocenteResponse obtener(@PathVariable Long id) {
        return docenteService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar un docente")
    @ApiResponse(responseCode = "201", description = "Docente registrado")
    public ResponseEntity<DocenteResponse> crear(@Valid @RequestBody DocenteRequest request) {
        DocenteResponse creado = docenteService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un docente")
    public DocenteResponse actualizar(@PathVariable Long id, @Valid @RequestBody DocenteRequest request) {
        return docenteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un docente")
    @ApiResponse(responseCode = "204", description = "Docente eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        docenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
