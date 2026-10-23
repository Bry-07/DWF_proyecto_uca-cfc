package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.RefreshToken;

/**
 * Acceso a datos de los refresh tokens.
 * Todavia no se usa: queda listo para la autenticacion con JWT de la Fase 3.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUsuarioId(Long usuarioId);
}
