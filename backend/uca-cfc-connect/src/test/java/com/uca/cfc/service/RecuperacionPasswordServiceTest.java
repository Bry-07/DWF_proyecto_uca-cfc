package com.uca.cfc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uca.cfc.dto.auth.MensajeResponse;
import com.uca.cfc.entity.PasswordResetToken;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.repository.PasswordResetTokenRepository;
import com.uca.cfc.repository.RefreshTokenRepository;
import com.uca.cfc.repository.UsuarioRepository;
import com.uca.cfc.util.TokenUtil;

@DisplayName("RecuperacionPasswordService")
class RecuperacionPasswordServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String EMAIL = "contabilidad@cfc.uca.edu.sv";

    private UsuarioRepository usuarioRepository;
    private PasswordResetTokenRepository tokenRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private CorreoService correoService;
    private RecuperacionPasswordService service;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        correoService = mock(CorreoService.class);

        Clock reloj = Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), ZoneId.of("America/El_Salvador"));
        service = new RecuperacionPasswordService(usuarioRepository, tokenRepository, refreshTokenRepository,
                passwordEncoder, correoService, reloj, 30, true);

        usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setEmail(EMAIL);
        usuario.setPasswordHash("hash-viejo");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoFalse(USUARIO_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("solicitar con un correo registrado guarda el hash del token y lo envía por correo")
    void solicitarCorreoRegistrado() {
        MensajeResponse respuesta = service.solicitar(EMAIL);

        ArgumentCaptor<PasswordResetToken> guardado = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(guardado.capture());
        assertNotNull(respuesta.tokenDePrueba());
        assertEquals(TokenUtil.sha256(respuesta.tokenDePrueba()), guardado.getValue().getTokenHash());
        assertEquals(LocalDateTime.of(2026, 10, 14, 9, 30), guardado.getValue().getFechaExpiracion());
        verify(correoService).enviarRecuperacion(usuario, respuesta.tokenDePrueba());
    }

    @Test
    @DisplayName("solicitar con un correo que no existe responde igual y no genera ningún token")
    void solicitarCorreoInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@cfc.uca.edu.sv")).thenReturn(Optional.empty());

        MensajeResponse respuesta = service.solicitar("nadie@cfc.uca.edu.sv");

        assertEquals(RecuperacionPasswordService.MENSAJE_SOLICITUD, respuesta.mensaje());
        assertNull(respuesta.tokenDePrueba());
        verify(correoService, never()).enviarRecuperacion(any(), anyString());
    }

    @Test
    @DisplayName("una solicitud nueva deja sin efecto los tokens anteriores")
    void solicitarInvalidaAnteriores() {
        PasswordResetToken anterior = tokenValido("anterior");
        when(tokenRepository.findByUsuarioIdAndUsadoFalse(USUARIO_ID)).thenReturn(List.of(anterior));

        service.solicitar(EMAIL);

        assertTrue(anterior.isUsado());
    }

    @Test
    @DisplayName("restablecer con un token válido cambia la contraseña, lo marca usado y cierra las sesiones")
    void restablecerCorrecto() {
        PasswordResetToken guardado = tokenValido("token-bueno");
        when(tokenRepository.findByTokenHash(TokenUtil.sha256("token-bueno"))).thenReturn(Optional.of(guardado));
        when(passwordEncoder.encode("NuevaClave#2026")).thenReturn("hash-nuevo");

        service.restablecer("token-bueno", "NuevaClave#2026");

        assertEquals("hash-nuevo", usuario.getPasswordHash());
        assertTrue(guardado.isUsado());
        verify(refreshTokenRepository).revocarTodosDelUsuario(USUARIO_ID);
    }

    @Test
    @DisplayName("un token ya usado no sirve una segunda vez")
    void restablecerTokenUsado() {
        PasswordResetToken guardado = tokenValido("token-bueno");
        guardado.setUsado(true);
        when(tokenRepository.findByTokenHash(TokenUtil.sha256("token-bueno"))).thenReturn(Optional.of(guardado));

        assertThrows(BusinessException.class, () -> service.restablecer("token-bueno", "NuevaClave#2026"));

        assertEquals("hash-viejo", usuario.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("un token vencido se rechaza")
    void restablecerTokenVencido() {
        PasswordResetToken guardado = tokenValido("token-bueno");
        guardado.setFechaExpiracion(LocalDateTime.of(2026, 10, 14, 8, 0));
        when(tokenRepository.findByTokenHash(TokenUtil.sha256("token-bueno"))).thenReturn(Optional.of(guardado));

        assertThrows(BusinessException.class, () -> service.restablecer("token-bueno", "NuevaClave#2026"));

        assertEquals("hash-viejo", usuario.getPasswordHash());
    }

    @Test
    @DisplayName("un token inventado se rechaza")
    void restablecerTokenDesconocido() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.restablecer("inventado", "NuevaClave#2026"));
    }

    private PasswordResetToken tokenValido(String valor) {
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(TokenUtil.sha256(valor));
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.of(2026, 10, 14, 9, 30));
        token.setUsado(false);
        return token;
    }
}
