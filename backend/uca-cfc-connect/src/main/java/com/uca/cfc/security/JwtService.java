package com.uca.cfc.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.uca.cfc.entity.Usuario;

/**
 * Firma los access tokens JWT (HS256).
 * El token lleva el correo como subject, el id del usuario (uid), su rol (rol) y un
 * identificador único (jti) que se usa para poder revocarlo al cerrar sesión.
 */
@Service
public class JwtService {

    public static final String EMISOR = JwtClaims.EMISOR;
    public static final String CLAIM_USUARIO_ID = JwtClaims.USUARIO_ID;
    public static final String CLAIM_ROL = JwtClaims.ROL;

    private final JwtEncoder encoder;
    private final Clock clock;
    private final long minutosDeVida;

    public JwtService(JwtEncoder encoder, Clock clock,
            @Value("${app.jwt.access-minutos:15}") long minutosDeVida) {
        this.encoder = encoder;
        this.clock = clock;
        this.minutosDeVida = minutosDeVida;
    }

    public TokenAcceso generarAcceso(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expiracion = ahora.plus(Duration.ofMinutes(minutosDeVida));
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getEmail())
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .id(jti)
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROL, usuario.getRol().getNombre())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();

        String valor = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenAcceso(valor, jti, expiracion, Duration.ofMinutes(minutosDeVida).toSeconds());
    }
}
