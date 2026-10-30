package com.uca.cfc.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import com.uca.cfc.entity.Rol;
import com.uca.cfc.entity.Usuario;
import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class UsuarioVigenteValidatorTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioVigenteValidator validator = new UsuarioVigenteValidator(usuarios);
    private final Usuario usuario = mock(Usuario.class);

    @BeforeEach
    void preparar() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        when(usuario.getRol()).thenReturn(rol);
        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        when(usuario.getEmail()).thenReturn("admin@example.com");
        when(usuario.getVersion()).thenReturn(2L);
        when(usuarios.findWithRolById(1L)).thenReturn(Optional.of(usuario));
    }

    private Jwt token(long version, String rol) {
        return Jwt.withTokenValue("jwt").header("alg", "HS256").subject("admin@example.com")
                .claim("jti", "sesion").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900))
                .claim("uid", 1L).claim("sv", version).claim("rol", rol).build();
    }

    @Test void aceptaCuentaVigente() { assertFalse(validator.validate(token(2, "ADMIN")).hasErrors()); }
    @Test void rechazaRevisionAnterior() { assertTrue(validator.validate(token(1, "ADMIN")).hasErrors()); }
    @Test void rechazaRolAnterior() { assertTrue(validator.validate(token(2, "CLIENTE")).hasErrors()); }
    @Test void rechazaCuentaInactiva() {
        when(usuario.getEstado()).thenReturn(EstadoUsuario.INACTIVO);
        assertTrue(validator.validate(token(2, "ADMIN")).hasErrors());
    }
    @Test void rechazaCuentaEliminada() {
        when(usuarios.findWithRolById(1L)).thenReturn(Optional.empty());
        assertTrue(validator.validate(token(2, "ADMIN")).hasErrors());
    }
    @Test void rechazaTokenSinRevision() {
        Jwt antiguo = Jwt.withTokenValue("jwt").header("alg", "HS256").claim("uid", 1L).build();
        assertTrue(validator.validate(antiguo).hasErrors());
    }
}
