package com.uca.cfc.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.uca.cfc.entity.enums.EstadoUsuario;
import com.uca.cfc.repository.UsuarioRepository;

/** Comprueba que la identidad y los permisos del JWT siguen vigentes en la base. */
public class UsuarioVigenteValidator implements OAuth2TokenValidator<Jwt> {
    private final UsuarioRepository usuarios;

    public UsuarioVigenteValidator(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object uid = jwt.getClaim(JwtClaims.USUARIO_ID);
        Object version = jwt.getClaim(JwtClaims.VERSION_SESION);
        if (uid instanceof Number id && version instanceof Number revision && jwt.getId() != null) {
            boolean vigente = usuarios.findWithRolById(id.longValue()).filter(usuario ->
                    usuario.getEstado() == EstadoUsuario.ACTIVO
                    && usuario.getVersion() == revision.longValue()
                    && usuario.getEmail().equals(jwt.getSubject())
                    && usuario.getRol().getNombre().equals(jwt.getClaimAsString(JwtClaims.ROL))).isPresent();
            if (vigente) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "La cuenta o sus permisos cambiaron. Inicie sesión nuevamente", null));
    }
}
