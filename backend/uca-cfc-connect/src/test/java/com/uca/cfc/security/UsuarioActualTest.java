package com.uca.cfc.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.Cliente;
import com.uca.cfc.exception.AccesoDenegadoException;
import com.uca.cfc.repository.ClienteRepository;

@DisplayName("UsuarioActual")
class UsuarioActualTest {

    private static final Long USUARIO_ID = 5L;
    private static final Long CLIENTE_ID = 3L;
    private static final Long OTRO_CLIENTE_ID = 99L;

    private ClienteRepository clienteRepository;
    private UsuarioActual usuarioActual;

    @BeforeEach
    void preparar() {
        clienteRepository = mock(ClienteRepository.class);
        usuarioActual = new UsuarioActual(clienteRepository);

        Cliente cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("un CLIENTE siempre consulta sus propios registros, aunque mande otro clienteId")
    void clienteSoloVeLoSuyo() {
        iniciarSesion("CLIENTE");

        assertEquals(CLIENTE_ID, usuarioActual.restringirCliente(OTRO_CLIENTE_ID));
        assertEquals(CLIENTE_ID, usuarioActual.restringirCliente(null));
    }

    @Test
    @DisplayName("el personal usa el filtro tal como lo mandó")
    void personalSinRestriccion() {
        iniciarSesion("RECEPCIONISTA");

        assertEquals(OTRO_CLIENTE_ID, usuarioActual.restringirCliente(OTRO_CLIENTE_ID));
        assertEquals(null, usuarioActual.restringirCliente(null));
    }

    @Test
    @DisplayName("un CLIENTE no puede crear solicitudes a nombre de otro cliente")
    void clienteNoCreaParaOtro() {
        iniciarSesion("CLIENTE");

        assertThrows(AccesoDenegadoException.class, () -> usuarioActual.verificarCliente(OTRO_CLIENTE_ID));
        assertDoesNotThrow(() -> usuarioActual.verificarCliente(CLIENTE_ID));
    }

    @Test
    @DisplayName("el personal sí puede registrar solicitudes para cualquier cliente")
    void personalCreaParaCualquiera() {
        iniciarSesion("RECEPCIONISTA");

        assertDoesNotThrow(() -> usuarioActual.verificarCliente(OTRO_CLIENTE_ID));
    }

    @Test
    @DisplayName("un CLIENTE no puede ver un registro que pertenece a otro cliente")
    void clienteNoVeRegistroAjeno() {
        iniciarSesion("CLIENTE");

        assertThrows(AccesoDenegadoException.class,
                () -> usuarioActual.verificarPropietario(new ResumenReferencia(OTRO_CLIENTE_ID, "Otra empresa")));
        assertDoesNotThrow(
                () -> usuarioActual.verificarPropietario(new ResumenReferencia(CLIENTE_ID, "Mi empresa")));
    }

    @Test
    @DisplayName("un CLIENTE sin ficha vinculada no puede consultar nada")
    void clienteSinFicha() {
        iniciarSesion("CLIENTE");
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(AccesoDenegadoException.class, () -> usuarioActual.restringirCliente(null));
    }

    @Test
    @DisplayName("sin sesión no hay usuario actual")
    void sinSesion() {
        assertThrows(AccesoDenegadoException.class, () -> usuarioActual.esCliente());
    }

    private void iniciarSesion(String rol) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("usuario@cfc.uca.edu.sv")
                .claim(JwtService.CLAIM_USUARIO_ID, USUARIO_ID)
                .claim(JwtService.CLAIM_ROL, rol)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
