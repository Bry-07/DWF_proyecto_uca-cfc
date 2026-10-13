package com.uca.cfc.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Pago;
import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;

/**
 * Los pagos con borrado lógico quedan fuera de todas estas consultas gracias a
 * la restricción {@code @SQLRestriction} declarada en la entidad.
 */
public interface PagoRepository extends JpaRepository<Pago, Long> {

    @EntityGraph(attributePaths = {"cliente", "comprobante"})
    Optional<Pago> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = {"cliente"})
    List<Pago> findByClienteIdOrderByFechaPagoDesc(Long clienteId);

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente"})
    @Query("""
            SELECT p FROM Pago p
            WHERE (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND (:concepto IS NULL OR p.concepto = :concepto)
              AND (:estado IS NULL OR p.estado = :estado)
              AND (:metodo IS NULL OR p.metodo = :metodo)
              AND (CAST(:desde AS date) IS NULL OR p.fechaPago >= :desde)
              AND (CAST(:hasta AS date) IS NULL OR p.fechaPago <= :hasta)
            """)
    Page<Pago> buscar(@Param("clienteId") Long clienteId, @Param("concepto") ConceptoPago concepto,
            @Param("estado") EstadoPago estado, @Param("metodo") MetodoPago metodo,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, Pageable pageable);

    /** Totales por estado para el reporte de estados financieros. */
    @Query("""
            SELECT p.estado, COUNT(p), COALESCE(SUM(p.montoEsperado), 0), COALESCE(SUM(p.montoPagado), 0)
            FROM Pago p
            WHERE (CAST(:desde AS date) IS NULL OR p.fechaPago >= :desde)
              AND (CAST(:hasta AS date) IS NULL OR p.fechaPago <= :hasta)
            GROUP BY p.estado
            """)
    List<Object[]> resumirPorEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT p.metodo, COUNT(p), COALESCE(SUM(p.montoPagado), 0)
            FROM Pago p
            WHERE (CAST(:desde AS date) IS NULL OR p.fechaPago >= :desde)
              AND (CAST(:hasta AS date) IS NULL OR p.fechaPago <= :hasta)
            GROUP BY p.metodo
            """)
    List<Object[]> resumirPorMetodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COALESCE(SUM(p.montoPagado), 0) FROM Pago p
            WHERE p.cliente.id = :clienteId
            """)
    BigDecimal totalPagadoPorCliente(@Param("clienteId") Long clienteId);
}
