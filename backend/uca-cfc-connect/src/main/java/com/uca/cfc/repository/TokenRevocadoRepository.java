package com.uca.cfc.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.TokenRevocado;

/** Acceso a datos de los access tokens con sesión cerrada. */
public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

    boolean existsByJti(String jti);

    /** Los tokens ya vencidos no hace falta recordarlos: igual serían rechazados por la fecha. */
    void deleteByFechaExpiracionBefore(LocalDateTime fecha);
}
