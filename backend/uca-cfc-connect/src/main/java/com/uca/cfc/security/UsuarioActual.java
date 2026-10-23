package com.uca.cfc.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.entity.enums.RolSistema;
import com.uca.cfc.exception.AccesoDenegadoException;
import com.uca.cfc.repository.ClienteRepository;

/**
 * Datos del usuario que hizo la petición, leídos del token.
 * Sirve para que un CLIENTE solo vea y cree sus propios registros: el resto de
 * roles trabaja sobre todos los clientes y no se les restringe nada.
 */
@Component
public class UsuarioActual {

    private final ClienteRepository clienteRepository;

    public UsuarioActual(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Long id() {
        Jwt jwt = jwtActual();
        Number uid = jwt.getClaim(JwtService.CLAIM_USUARIO_ID);
        return uid.longValue();
    }

    public String email() {
        return jwtActual().getSubject();
    }

    public boolean esCliente() {
        return RolSistema.CLIENTE.name().equals(jwtActual().getClaimAsString(JwtService.CLAIM_ROL));
    }

    /** Id del cliente vinculado a la cuenta del usuario actual. */
    public Long clienteId() {
        return clienteRepository.findByUsuarioId(id())
                .map(cliente -> cliente.getId())
                .orElseThrow(() -> new AccesoDenegadoException(
                        "Su cuenta no está vinculada a ningún cliente. Comuníquese con recepción"));
    }

    /**
     * En los listados: un CLIENTE siempre consulta sus propios registros, sin
     * importar el clienteId que haya mandado. Los demás roles usan el filtro tal cual.
     */
    public Long restringirCliente(Long clienteIdSolicitado) {
        return esCliente() ? clienteId() : clienteIdSolicitado;
    }

    /** Al crear: un CLIENTE no puede registrar solicitudes a nombre de otro cliente. */
    public void verificarCliente(Long clienteIdDelRequest) {
        if (esCliente() && !clienteId().equals(clienteIdDelRequest)) {
            throw new AccesoDenegadoException("Solo puede registrar solicitudes a su propio nombre");
        }
    }

    /** Al consultar un registro: un CLIENTE solo puede ver los que son suyos. */
    public void verificarPropietario(ResumenReferencia cliente) {
        if (esCliente() && (cliente == null || !clienteId().equals(cliente.id()))) {
            throw new AccesoDenegadoException("No tiene permiso para consultar este registro");
        }
    }

    private Jwt jwtActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new AccesoDenegadoException("No hay una sesión activa");
    }
}
