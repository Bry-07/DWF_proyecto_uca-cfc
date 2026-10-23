package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.Comprobante;

/**
 * Acceso a datos de los comprobantes de pago.
 * countByNumeroStartingWith se usa para generar el correlativo anual
 * (CFC-2026-000001, CFC-2026-000002, y asi).
 */
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByPagoId(Long pagoId);

    boolean existsByPagoId(Long pagoId);

    long countByNumeroStartingWith(String prefijo);
}
