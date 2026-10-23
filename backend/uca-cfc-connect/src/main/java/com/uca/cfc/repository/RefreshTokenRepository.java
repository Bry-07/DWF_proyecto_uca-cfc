package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.RefreshToken;

/**
 * Acceso a datos de los refresh tokens.
 * Todavia no se usa: queda listo para la autenticacion con JWT de la Fase 3.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUsuarioId(Long usuarioId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revocado = true WHERE t.usuario.id = :usuarioId AND t.revocado = false")
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId);
}
