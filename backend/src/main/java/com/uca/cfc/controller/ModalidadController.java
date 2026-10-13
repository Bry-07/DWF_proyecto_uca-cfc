package com.uca.cfc.controller;

import java.util.List;

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

import com.uca.cfc.dto.modalidad.ModalidadRequest;
import com.uca.cfc.dto.modalidad.ModalidadResponse;
import com.uca.cfc.entity.enums.TipoModalidad;
import com.uca.cfc.service.ModalidadService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de las modalidades (Presencial, Virtual, Hibrida).
 * Como son pocas y casi nunca cambian, el listado no se pagina:
 * se devuelven todas de una vez.
 */
@RestController
@RequestMapping("/api/v1/modalidades")
@Tag(name = "Modalidades", description = "Modalidades en que se imparte la oferta académica")
public class ModalidadController {

    private final ModalidadService modalidadService;

    public ModalidadController(ModalidadService modalidadService) {
        this.modalidadService = modalidadService;
    }

    @GetMapping
    @Operation(summary = "Listar modalidades",
            description = "Las modalidades son pocas y estables, por lo que se devuelven completas sin paginar.")
    public List<ModalidadResponse> listar(@RequestParam(required = false) TipoModalidad tipo) {
        return modalidadService.listar(tipo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una modalidad")
    public ModalidadResponse obtener(@PathVariable Long id) {
        return modalidadService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear una modalidad")
    @ApiResponse(responseCode = "201", description = "Modalidad creada")
    public ResponseEntity<ModalidadResponse> crear(@Valid @RequestBody ModalidadRequest request) {
        ModalidadResponse creada = modalidadService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una modalidad")
    public ModalidadResponse actualizar(@PathVariable Long id, @Valid @RequestBody ModalidadRequest request) {
        return modalidadService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una modalidad")
    @ApiResponse(responseCode = "204", description = "Modalidad eliminada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        modalidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
