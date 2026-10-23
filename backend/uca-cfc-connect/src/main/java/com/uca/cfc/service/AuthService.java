package com.uca.cfc.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uca.cfc.dto.auth.LoginRequest;
import com.uca.cfc.dto.auth.PerfilResponse;
import com.uca.cfc.dto.auth.RefreshRequest;
import com.uca.cfc.dto.auth.TokenResponse;
import com.uca.cfc.entity.RefreshToken;
import com.uca.cfc.entity.TokenRevocado;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.exception.AccesoDenegadoException;
import com.uca.cfc.exception.CredencialesInvalidasException;
import com.uca.cfc.exception.ResourceNotFoundException;
import com.uca.cfc.repository.ClienteRepository;
import com.uca.cfc.repository.RefreshTokenRepository;
import com.uca.cfc.repository.TokenRevocadoRepository;
import com.uca.cfc.repository.UsuarioRepository;
import com.uca.cfc.security.JwtService;
import com.uca.cfc.security.TokenAcceso;
import com.uca.cfc.util.TokenUtil;

/**
 * Inicio y cierre de sesión.
 * El access token (JWT) dura poco y no se guarda. El refresh token es un valor
 * aleatorio del que solo se guarda el hash; cada vez que se usa se reemplaza por
 * uno nuevo (rotación), y si alguien reutiliza uno ya usado se cierran todas las
 * sesiones de ese usuario porque es señal de que el token fue copiado.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos";
    private static final String REFRESH_INVALIDO = "El refresh token no es válido o ya expiró";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final long diasRefresh;

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RefreshTokenRepository refreshTokenRepository, TokenRevocadoRepository tokenRevocadoRepository,
            JwtService jwtService, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${app.jwt.refresh-dias:7}") long diasRefresh) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenRevocadoRepository = tokenRevocadoRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.diasRefresh = diasRefresh;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        // Mismo mensaje si el correo no existe o la contraseña falla, para no revelar cuáles correos están registrados
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }
        verificarActivo(usuario);
        return emitirTokens(usuario);
    }

    /**
     * noRollbackFor: cuando se detecta la reutilización de un token, las sesiones se
     * cierran y después se lanza el error; sin esto la excepción desharía el cierre.
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public TokenResponse refrescar(RefreshRequest request) {
        RefreshToken guardado = refreshTokenRepository.findByToken(TokenUtil.sha256(request.refreshToken()))
                .orElseThrow(() -> new CredencialesInvalidasException(REFRESH_INVALIDO));
        Long usuarioId = guardado.getUsuario().getId();

        if (guardado.isRevocado()) {
            refreshTokenRepository.revocarTodosDelUsuario(usuarioId);
            throw new CredencialesInvalidasException(REFRESH_INVALIDO);
        }
        if (guardado.getFechaExpiracion().isBefore(LocalDateTime.now(clock))) {
            throw new CredencialesInvalidasException(REFRESH_INVALIDO);
        }

        Usuario usuario = usuarioRepository.findWithRolById(usuarioId)
                .orElseThrow(() -> new CredencialesInvalidasException(REFRESH_INVALIDO));
        verificarActivo(usuario);

        guardado.setRevocado(true);
        refreshTokenRepository.save(guardado);
        return emitirTokens(usuario);
    }

    /**
     * Cierra la sesión: el access token queda en la lista de revocados hasta que
     * venza y el refresh token deja de servir. Sin refresh token se cierran todas
     * las sesiones del usuario.
     */
    @Transactional
    public void cerrarSesion(String jti, Instant expiracion, Long usuarioId, String refreshToken) {
        tokenRevocadoRepository.deleteByFechaExpiracionBefore(LocalDateTime.now(clock));

        if (!tokenRevocadoRepository.existsByJti(jti)) {
            TokenRevocado revocado = new TokenRevocado();
            revocado.setJti(jti);
            revocado.setFechaExpiracion(LocalDateTime.ofInstant(expiracion, clock.getZone()));
            tokenRevocadoRepository.save(revocado);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            refreshTokenRepository.revocarTodosDelUsuario(usuarioId);
            return;
        }
        refreshTokenRepository.findByToken(TokenUtil.sha256(refreshToken))
                .filter(token -> token.getUsuario().getId().equals(usuarioId))
                .ifPresent(token -> {
                    token.setRevocado(true);
                    refreshTokenRepository.save(token);
                });
    }

    public PerfilResponse perfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findWithRolById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        Long clienteId = clienteRepository.findByUsuarioId(usuarioId)
                .map(cliente -> cliente.getId())
                .orElse(null);
        return new PerfilResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                usuario.getRol().getNombre(), clienteId);
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        TokenAcceso acceso = jwtService.generarAcceso(usuario);

        String refresh = TokenUtil.generarAleatorio();
        RefreshToken entidad = new RefreshToken();
        entidad.setToken(TokenUtil.sha256(refresh));
        entidad.setUsuario(usuario);
        entidad.setFechaExpiracion(LocalDateTime.now(clock).plusDays(diasRefresh));
        entidad.setRevocado(false);
        refreshTokenRepository.save(entidad);

        return new TokenResponse(acceso.valor(), "Bearer", acceso.segundosDeVida(), refresh,
                new TokenResponse.UsuarioSesion(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                        usuario.getRol().getNombre()));
    }

    private void verificarActivo(Usuario usuario) {
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new AccesoDenegadoException("La cuenta está inactiva. Comuníquese con el administrador");
        }
    }
}
