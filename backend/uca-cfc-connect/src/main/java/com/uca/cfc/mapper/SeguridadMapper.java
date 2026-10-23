package com.uca.cfc.mapper;

import org.springframework.stereotype.Component;

import com.uca.cfc.dto.common.ResumenReferencia;
import com.uca.cfc.dto.rol.RolResponse;
import com.uca.cfc.dto.usuario.UsuarioResponse;
import com.uca.cfc.entity.Rol;
import com.uca.cfc.entity.Usuario;

/**
 * Convierte usuarios y roles en DTOs.
 * Importante: el DTO de usuario no tiene campo de contrasena, por eso el hash
 * nunca sale en las respuestas de la API.
 */
@Component
public class SeguridadMapper {

    public RolResponse aResponse(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.esDelSistema());
    }

    public UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getEstado(),
                new ResumenReferencia(usuario.getRol().getId(), usuario.getRol().getNombre()),
                usuario.getCreatedAt(), usuario.getUpdatedAt());
    }
}
