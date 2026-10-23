package com.uca.cfc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uca.cfc.dto.auth.LoginRequest;
import com.uca.cfc.dto.auth.LogoutRequest;
import com.uca.cfc.dto.auth.MensajeResponse;
import com.uca.cfc.dto.auth.PerfilResponse;
import com.uca.cfc.dto.auth.RecuperarPasswordRequest;
import com.uca.cfc.dto.auth.RefreshRequest;
import com.uca.cfc.dto.auth.RegistroClienteRequest;
import com.uca.cfc.dto.auth.RestablecerPasswordRequest;
import com.uca.cfc.dto.auth.TokenResponse;
import com.uca.cfc.dto.cliente.ClienteResponse;
import com.uca.cfc.security.UsuarioActual;
import com.uca.cfc.service.AuthService;
import com.uca.cfc.service.RecuperacionPasswordService;
import com.uca.cfc.service.RegistroService;
import com.uca.cfc.util.UbicacionRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Autenticación: login, renovación del token, cierre de sesión, registro de
 * clientes y recuperación de contraseña. Salvo /logout y /me, todas son públicas.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio y cierre de sesión con JWT, registro y recuperación de contraseña")
public class AuthController {

    private final AuthService authService;
    private final RegistroService registroService;
    private final RecuperacionPasswordService recuperacionService;
    private final UsuarioActual usuarioActual;

    public AuthController(AuthService authService, RegistroService registroService,
            RecuperacionPasswordService recuperacionService, UsuarioActual usuarioActual) {
        this.authService = authService;
        this.registroService = registroService;
        this.recuperacionService = recuperacionService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve un access token (JWT, dura 15 minutos) y un refresh token. "
                    + "El access token se envía como `Authorization: Bearer {token}`.")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos")
    @ApiResponse(responseCode = "403", description = "La cuenta está inactiva")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar el access token",
            description = "Cambia un refresh token por un par de tokens nuevo. El refresh token usado deja de servir.")
    @ApiResponse(responseCode = "401", description = "El refresh token no es válido, expiró o ya fue usado")
    public TokenResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authService.refrescar(request);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cerrar sesión",
            description = "Invalida el access token actual y el refresh token enviado. "
                    + "Si no se envía refresh token, se cierran todas las sesiones del usuario.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) LogoutRequest request) {
        String refresh = request == null ? null : request.refreshToken();
        authService.cerrarSesion(jwt.getId(), jwt.getExpiresAt(), usuarioActual.id(), refresh);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Datos del usuario autenticado")
    public PerfilResponse perfil() {
        return authService.perfil(usuarioActual.id());
    }

    @PostMapping("/registro")
    @SecurityRequirements
    @Operation(summary = "Registrarse como cliente",
            description = "Crea la cuenta (rol CLIENTE) y la ficha de cliente en un solo paso. "
                    + "Después se inicia sesión con /auth/login.")
    @ApiResponse(responseCode = "201", description = "Cliente registrado")
    @ApiResponse(responseCode = "409", description = "El correo, DUI o NIT ya están registrados")
    public ResponseEntity<ClienteResponse> registro(@Valid @RequestBody RegistroClienteRequest request) {
        ClienteResponse creado = registroService.registrar(request);
        return ResponseEntity.created(UbicacionRecurso.de(creado.id())).body(creado);
    }

    @PostMapping("/recuperar-password")
    @SecurityRequirements
    @Operation(summary = "Solicitar la recuperación de contraseña",
            description = "Responde lo mismo exista o no el correo. El token se envía por correo "
                    + "y vence a los 30 minutos.")
    public MensajeResponse recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest request) {
        return recuperacionService.solicitar(request.email());
    }

    @PostMapping("/restablecer-password")
    @SecurityRequirements
    @Operation(summary = "Restablecer la contraseña con el token recibido",
            description = "El token sirve una sola vez. Al cambiar la contraseña se cierran las sesiones abiertas.")
    @ApiResponse(responseCode = "422", description = "El token no es válido o ya expiró")
    public MensajeResponse restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        return recuperacionService.restablecer(request.token(), request.nuevaPassword());
    }
}
