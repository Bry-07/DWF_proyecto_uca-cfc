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
import com.uca.cfc.dto.usuario.CambioEstadoUsuarioRequest;
import com.uca.cfc.dto.usuario.UsuarioRequest;
import com.uca.cfc.dto.usuario.UsuarioResponse;
import com.uca.cfc.dto.usuario.UsuarioUpdateRequest;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.service.UsuarioService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador de los usuarios del sistema.
 * Las respuestas nunca incluyen la contrasena ni su hash, porque el DTO de
 * respuesta simplemente no tiene ese campo.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Cuentas de acceso al sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios",
            description = "Las respuestas nunca incluyen la contraseña ni su hash.")
    public PageResponse<UsuarioResponse> listar(
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) EstadoUsuario estado,
            @Parameter(description = "Búsqueda por nombre o correo") @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarioService.listar(rolId, estado, texto, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un usuario",
            description = "La contraseña se almacena cifrada con BCrypt; nunca en texto plano.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese correo")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crear(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario",
            description = "Si no se envía contraseña, se conserva la actual.")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar un usuario")
    public UsuarioResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoUsuarioRequest request) {
        return usuarioService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un usuario")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
