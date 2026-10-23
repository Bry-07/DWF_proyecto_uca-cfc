package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uca.cfc.dto.auth.LoginRequest;
import com.uca.cfc.dto.auth.RefreshRequest;
import com.uca.cfc.dto.auth.TokenResponse;
import com.uca.cfc.entity.RefreshToken;
import com.uca.cfc.entity.Rol;
import com.uca.cfc.entity.TokenRevocado;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.exception.AccesoDenegadoException;
import com.uca.cfc.exception.CredencialesInvalidasException;
import com.uca.cfc.repository.ClienteRepository;
import com.uca.cfc.repository.RefreshTokenRepository;
import com.uca.cfc.repository.TokenRevocadoRepository;
import com.uca.cfc.repository.UsuarioRepository;
import com.uca.cfc.security.JwtService;
import com.uca.cfc.security.TokenAcceso;
import com.uca.cfc.util.TokenUtil;

@DisplayName("AuthService")
class AuthServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String EMAIL = "recepcion@cfc.uca.edu.sv";
    private static final String PASSWORD = "Recepcion#2026.Cfc";

    private UsuarioRepository usuarioRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private TokenRevocadoRepository tokenRevocadoRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthService service;

    private Clock reloj;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        tokenRevocadoRepository = mock(TokenRevocadoRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);

        reloj = Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), ZoneId.of("America/El_Salvador"));
        service = new AuthService(usuarioRepository, clienteRepository, refreshTokenRepository,
                tokenRevocadoRepository, jwtService, passwordEncoder, reloj, 7);

        Rol rol = new Rol();
        rol.setNombre("RECEPCIONISTA");
        usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Rocío Cortez");
        usuario.setEmail(EMAIL);
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findWithRolById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD, "hash-bcrypt")).thenReturn(true);
        when(jwtService.generarAcceso(usuario)).thenReturn(
                new TokenAcceso("jwt-firmado", "jti-1", reloj.instant().plusSeconds(900), 900));
    }

    @Test
    @DisplayName("login correcto devuelve el JWT y guarda solo el hash del refresh token")
    void loginCorrecto() {
        TokenResponse respuesta = service.login(new LoginRequest(EMAIL, PASSWORD));

        assertEquals("jwt-firmado", respuesta.accessToken());
        assertEquals("Bearer", respuesta.tokenType());
        assertEquals(900, respuesta.expiresIn());
        assertEquals("RECEPCIONISTA", respuesta.usuario().rol());

        ArgumentCaptor<RefreshToken> guardado = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(guardado.capture());
        assertNotEquals(respuesta.refreshToken(), guardado.getValue().getToken());
        assertEquals(TokenUtil.sha256(respuesta.refreshToken()), guardado.getValue().getToken());
        assertEquals(LocalDateTime.of(2026, 10, 21, 9, 0), guardado.getValue().getFechaExpiracion());
    }

    @Test
    @DisplayName("login con contraseña incorrecta responde 401 y no emite tokens")
    void loginPasswordIncorrecta() {
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest(EMAIL, "OtraClave#2026")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verify(jwtService, never()).generarAcceso(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("login con un correo que no existe da el mismo mensaje que una contraseña incorrecta")
    void loginCorreoInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@cfc.uca.edu.sv")).thenReturn(Optional.empty());

        CredencialesInvalidasException sinCorreo = assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("nadie@cfc.uca.edu.sv", PASSWORD)));
        CredencialesInvalidasException sinPassword = assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest(EMAIL, "OtraClave#2026")));

        assertEquals(sinPassword.getMessage(), sinCorreo.getMessage());
    }

    @Test
    @DisplayName("un usuario INACTIVO no puede iniciar sesión aunque la contraseña sea correcta")
    void loginUsuarioInactivo() {
        usuario.setEstado(EstadoUsuario.INACTIVO);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> service.login(new LoginRequest(EMAIL, PASSWORD)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verify(jwtService, never()).generarAcceso(any());
    }

    @Test
    @DisplayName("refrescar entrega tokens nuevos y revoca el refresh token usado")
    void refrescarRota() {
        RefreshToken guardado = refreshTokenVigente("refresh-viejo");
        when(refreshTokenRepository.findByToken(TokenUtil.sha256("refresh-viejo")))
                .thenReturn(Optional.of(guardado));

        TokenResponse respuesta = service.refrescar(new RefreshRequest("refresh-viejo"));

        assertTrue(guardado.isRevocado());
        assertNotEquals("refresh-viejo", respuesta.refreshToken());
        assertEquals("jwt-firmado", respuesta.accessToken());
    }

    @Test
    @DisplayName("reutilizar un refresh token ya usado cierra todas las sesiones del usuario")
    void refrescarReutilizado() {
        RefreshToken guardado = refreshTokenVigente("refresh-viejo");
        guardado.setRevocado(true);
        when(refreshTokenRepository.findByToken(TokenUtil.sha256("refresh-viejo")))
                .thenReturn(Optional.of(guardado));

        assertThrows(CredencialesInvalidasException.class,
                () -> service.refrescar(new RefreshRequest("refresh-viejo")));

        verify(refreshTokenRepository).revocarTodosDelUsuario(USUARIO_ID);
        verify(jwtService, never()).generarAcceso(any());
    }

    @Test
    @DisplayName("un refresh token vencido se rechaza")
    void refrescarVencido() {
        RefreshToken guardado = refreshTokenVigente("refresh-viejo");
        guardado.setFechaExpiracion(LocalDateTime.of(2026, 10, 1, 0, 0));
        when(refreshTokenRepository.findByToken(TokenUtil.sha256("refresh-viejo")))
                .thenReturn(Optional.of(guardado));

        assertThrows(CredencialesInvalidasException.class,
                () -> service.refrescar(new RefreshRequest("refresh-viejo")));
        assertFalse(guardado.isRevocado());
    }

    @Test
    @DisplayName("un refresh token desconocido se rechaza")
    void refrescarDesconocido() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> service.refrescar(new RefreshRequest("inventado")));
    }

    @Test
    @DisplayName("cerrar sesión guarda el jti como revocado y revoca el refresh token enviado")
    void cerrarSesionConRefresh() {
        RefreshToken guardado = refreshTokenVigente("mi-refresh");
        when(tokenRevocadoRepository.existsByJti("jti-1")).thenReturn(false);
        when(refreshTokenRepository.findByToken(TokenUtil.sha256("mi-refresh")))
                .thenReturn(Optional.of(guardado));

        service.cerrarSesion("jti-1", reloj.instant().plusSeconds(600), USUARIO_ID, "mi-refresh");

        ArgumentCaptor<TokenRevocado> revocado = ArgumentCaptor.forClass(TokenRevocado.class);
        verify(tokenRevocadoRepository).save(revocado.capture());
        assertEquals("jti-1", revocado.getValue().getJti());
        assertTrue(guardado.isRevocado());
        verify(refreshTokenRepository, never()).revocarTodosDelUsuario(anyLong());
    }

    @Test
    @DisplayName("cerrar sesión sin refresh token cierra todas las sesiones del usuario")
    void cerrarSesionSinRefresh() {
        service.cerrarSesion("jti-1", reloj.instant().plusSeconds(600), USUARIO_ID, null);

        verify(refreshTokenRepository).revocarTodosDelUsuario(USUARIO_ID);
    }

    @Test
    @DisplayName("cerrar sesión no puede revocar el refresh token de otro usuario")
    void cerrarSesionRefreshAjeno() {
        RefreshToken ajeno = refreshTokenVigente("refresh-ajeno");
        Usuario otro = new Usuario();
        otro.setId(99L);
        ajeno.setUsuario(otro);
        when(refreshTokenRepository.findByToken(TokenUtil.sha256("refresh-ajeno")))
                .thenReturn(Optional.of(ajeno));

        service.cerrarSesion("jti-1", reloj.instant().plusSeconds(600), USUARIO_ID, "refresh-ajeno");

        assertFalse(ajeno.isRevocado());
    }

    private RefreshToken refreshTokenVigente(String valor) {
        RefreshToken token = new RefreshToken();
        token.setToken(TokenUtil.sha256(valor));
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.of(2026, 10, 20, 0, 0));
        token.setRevocado(false);
        return token;
    }
}
