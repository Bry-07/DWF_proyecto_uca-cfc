package com.uca.cfc.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.auth.MensajeResponse;
import com.uca.cfc.entity.PasswordResetToken;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.exception.BusinessException;
import com.uca.cfc.repository.PasswordResetTokenRepository;
import com.uca.cfc.repository.RefreshTokenRepository;
import com.uca.cfc.repository.UsuarioRepository;
import com.uca.cfc.util.TokenUtil;

/**
 * Recuperación de contraseña en dos pasos: se pide el token con el correo y luego
 * se usa para fijar la contraseña nueva. El token vale una sola vez y vence en pocos
 * minutos. Al cambiar la contraseña se cierran todas las sesiones abiertas.
 */
@Service
@Transactional(readOnly = true)
public class RecuperacionPasswordService {

    static final String MENSAJE_SOLICITUD =
            "Si el correo está registrado, recibirá las instrucciones para restablecer su contraseña";
    static final String TOKEN_INVALIDO = "El enlace de recuperación no es válido o ya expiró";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correoService;
    private final Clock clock;
    private final long minutosDeVida;
    private final boolean mostrarToken;

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder, CorreoService correoService, Clock clock,
            @Value("${app.seguridad.recuperacion-minutos:30}") long minutosDeVida,
            @Value("${app.seguridad.mostrar-token-recuperacion:false}") boolean mostrarToken) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoService = correoService;
        this.clock = clock;
        this.minutosDeVida = minutosDeVida;
        this.mostrarToken = mostrarToken;
    }

    /** Responde siempre lo mismo, exista o no el correo, para no revelar quién tiene cuenta. */
    @Transactional
    public MensajeResponse solicitar(String email) {
        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO);
        if (usuario.isEmpty()) {
            return new MensajeResponse(MENSAJE_SOLICITUD, null);
        }

        // Una solicitud nueva deja sin efecto las anteriores
        for (PasswordResetToken anterior : tokenRepository.findByUsuarioIdAndUsadoFalse(usuario.get().getId())) {
            anterior.setUsado(true);
            tokenRepository.save(anterior);
        }

        String token = TokenUtil.generarAleatorio();
        PasswordResetToken nuevo = new PasswordResetToken();
        nuevo.setTokenHash(TokenUtil.sha256(token));
        nuevo.setUsuario(usuario.get());
        nuevo.setFechaExpiracion(LocalDateTime.now(clock).plusMinutes(minutosDeVida));
        nuevo.setUsado(false);
        tokenRepository.save(nuevo);

        correoService.enviarRecuperacion(usuario.get(), token);
        return new MensajeResponse(MENSAJE_SOLICITUD, mostrarToken ? token : null);
    }

    @Transactional
    public MensajeResponse restablecer(String token, String nuevaPassword) {
        PasswordResetToken guardado = tokenRepository.findByTokenHash(TokenUtil.sha256(token))
                .orElseThrow(() -> new BusinessException(TOKEN_INVALIDO));
        if (guardado.isUsado() || guardado.getFechaExpiracion().isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException(TOKEN_INVALIDO);
        }

        Usuario usuario = guardado.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        guardado.setUsado(true);
        tokenRepository.save(guardado);
        refreshTokenRepository.revocarTodosDelUsuario(usuario.getId());

        return new MensajeResponse("Contraseña actualizada. Ya puede iniciar sesión", null);
    }
}
