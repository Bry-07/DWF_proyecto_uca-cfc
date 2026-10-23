package com.uca.cfc.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.uca.cfc.entity.Rol;
import com.uca.cfc.entity.Usuario;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRETO = "clave-solo-para-pruebas-automatizadas-2026-uca-cfc";

    private SecretKey clave;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        clave = new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        Rol rol = new Rol();
        rol.setNombre("CONTABILIDAD");
        usuario = new Usuario();
        usuario.setId(4L);
        usuario.setEmail("contabilidad@cfc.uca.edu.sv");
        usuario.setRol(rol);
    }

    @Test
    @DisplayName("el token lleva el correo, el id, el rol, el emisor y un jti único")
    void tokenConLosDatosDelUsuario() {
        JwtService service = servicio(Clock.systemDefaultZone(), 15);

        TokenAcceso token = service.generarAcceso(usuario);
        Jwt jwt = decodificador(clave).decode(token.valor());

        assertEquals("contabilidad@cfc.uca.edu.sv", jwt.getSubject());
        assertEquals(4L, ((Number) jwt.getClaim(JwtService.CLAIM_USUARIO_ID)).longValue());
        assertEquals("CONTABILIDAD", jwt.getClaimAsString(JwtService.CLAIM_ROL));
        assertEquals(JwtService.EMISOR, jwt.getClaimAsString("iss"));
        assertEquals(token.jti(), jwt.getId());
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertEquals(900, token.segundosDeVida());
    }

    @Test
    @DisplayName("dos tokens del mismo usuario tienen jti distinto")
    void jtiUnico() {
        JwtService service = servicio(Clock.systemDefaultZone(), 15);

        assertEquals(false, service.generarAcceso(usuario).jti().equals(service.generarAcceso(usuario).jti()));
    }

    @Test
    @DisplayName("un token firmado con otra clave se rechaza")
    void firmaIncorrecta() {
        String token = servicio(Clock.systemDefaultZone(), 15).generarAcceso(usuario).valor();
        SecretKey otraClave = new SecretKeySpec(
                "otra-clave-distinta-de-mas-de-32-caracteres-1234".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        assertThrows(JwtException.class, () -> decodificador(otraClave).decode(token));
    }

    @Test
    @DisplayName("un token vencido se rechaza")
    void tokenVencido() {
        Clock haceUnAnio = Clock.fixed(Instant.now().minus(Duration.ofDays(365)), ZoneId.of("America/El_Salvador"));
        String token = servicio(haceUnAnio, 15).generarAcceso(usuario).valor();

        assertThrows(JwtException.class, () -> decodificador(clave).decode(token));
    }

    private JwtService servicio(Clock reloj, long minutos) {
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(clave)), reloj, minutos);
    }

    private JwtDecoder decodificador(SecretKey claveDeFirma) {
        return NimbusJwtDecoder.withSecretKey(claveDeFirma).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
