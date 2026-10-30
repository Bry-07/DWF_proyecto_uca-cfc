package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.Comprobante;

/** Acceso a datos de los comprobantes de pago. */
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    Optional<Comprobante> findByPagoId(Long pagoId);

    boolean existsByPagoId(Long pagoId);

}
