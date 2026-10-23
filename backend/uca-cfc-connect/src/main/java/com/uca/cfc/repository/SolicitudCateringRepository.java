package com.uca.cfc.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.SolicitudCatering;
import com.uca.cfc.entity.enums.EstadoReserva;

/**
 * Acceso a datos de las solicitudes de catering.
 */
public interface SolicitudCateringRepository extends JpaRepository<SolicitudCatering, Long> {

    boolean existsByServicioId(Long servicioId);

    boolean existsByClienteId(Long clienteId);

    boolean existsByEspacioId(Long espacioId);

    @EntityGraph(attributePaths = {"cliente", "servicio", "espacio"})
    Optional<SolicitudCatering> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = {"cliente", "servicio", "espacio"})
    @Query("""
            SELECT s FROM SolicitudCatering s
            WHERE (:clienteId IS NULL OR s.cliente.id = :clienteId)
              AND (:servicioId IS NULL OR s.servicio.id = :servicioId)
              AND (:estado IS NULL OR s.estado = :estado)
              AND (CAST(:desde AS date) IS NULL OR s.fecha >= :desde)
              AND (CAST(:hasta AS date) IS NULL OR s.fecha <= :hasta)
            """)
    Page<SolicitudCatering> buscar(@Param("clienteId") Long clienteId, @Param("servicioId") Long servicioId,
            @Param("estado") EstadoReserva estado, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta, Pageable pageable);
}
