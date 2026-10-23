package com.uca.cfc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.PasswordResetToken;

/** Acceso a datos de los tokens de recuperación de contraseña. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUsuarioIdAndUsadoFalse(Long usuarioId);
}
