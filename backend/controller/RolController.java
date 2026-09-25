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
import org.springframework.web.bind.annotation.RestController;

import com.uca.cfc.dto.rol.RolRequest;
import com.uca.cfc.dto.rol.RolResponse;
import com.uca.cfc.service.RolService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los roles (ADMIN, RECEPCIONISTA, CLIENTE, CONTABILIDAD).
 * Los cuatro roles base no se pueden renombrar ni eliminar, porque son los
 * que sostienen los permisos del sistema.
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Roles de acceso: ADMIN, RECEPCIONISTA, CLIENTE y CONTABILIDAD")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @Operation(summary = "Listar roles")
    public List<RolResponse> listar() {
        return rolService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un rol")
    public RolResponse obtener(@PathVariable Long id) {
        return rolService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un rol")
    @ApiResponse(responseCode = "201", description = "Rol creado")
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody RolRequest request) {
        RolResponse creado = rolService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un rol",
            description = "De los cuatro roles base solo puede actualizarse la descripción.")
    @ApiResponse(responseCode = "422", description = "Se intentó renombrar un rol base del sistema")
    public RolResponse actualizar(@PathVariable Long id, @Valid @RequestBody RolRequest request) {
        return rolService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un rol",
            description = "Los roles base del sistema y los roles con usuarios asignados no se eliminan.")
    @ApiResponse(responseCode = "204", description = "Rol eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
