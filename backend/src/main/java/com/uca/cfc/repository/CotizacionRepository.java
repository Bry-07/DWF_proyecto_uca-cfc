package com.uca.cfc.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Cotizacion;
import com.uca.cfc.entity.enums.EstadoCotizacion;
import com.uca.cfc.entity.enums.TipoCotizacion;

/**
 * Acceso a datos de las cotizaciones.
 * findWithItemsById trae la cotizacion junto con sus items en una sola consulta.
 */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @EntityGraph(attributePaths = {"cliente", "items"})
    Optional<Cotizacion> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"cliente", "items"})
    List<Cotizacion> findByClienteIdOrderByFechaSolicitudDesc(Long clienteId);

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "items"})
    @Query("""
            SELECT DISTINCT c FROM Cotizacion c
            WHERE (:clienteId IS NULL OR c.cliente.id = :clienteId)
              AND (:tipo IS NULL OR c.tipo = :tipo)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (CAST(:desde AS date) IS NULL OR c.fechaSolicitud >= :desde)
              AND (CAST(:hasta AS date) IS NULL OR c.fechaSolicitud <= :hasta)
            """)
    Page<Cotizacion> buscar(@Param("clienteId") Long clienteId, @Param("tipo") TipoCotizacion tipo,
            @Param("estado") EstadoCotizacion estado, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta, Pageable pageable);
}
