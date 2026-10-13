package com.uca.cfc.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Alquiler;
import com.uca.cfc.entity.enums.EstadoReserva;

/**
 * Acceso a datos de los alquileres.
 * buscarSolapamientos es la consulta clave del modulo: devuelve las reservas
 * activas de un espacio que chocan con el horario que se quiere reservar.
 * Las canceladas no cuentan, porque esas ya liberaron el espacio.
 */
public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

    /**
     * Reservas activas de un espacio que se superponen con el rango indicado.
     * Regla: nuevoInicio &lt; existenteFin AND nuevoFin &gt; existenteInicio.
     * Las reservas canceladas no bloquean el espacio.
     */
    @EntityGraph(attributePaths = {"cliente", "espacio"})
    @Query("""
            SELECT a FROM Alquiler a
            WHERE a.espacio.id = :espacioId
              AND a.estado <> com.uca.cfc.entity.enums.EstadoReserva.CANCELADA
              AND a.fechaHoraInicio < :fin
              AND a.fechaHoraFin > :inicio
              AND (:idExcluido IS NULL OR a.id <> :idExcluido)
            """)
    List<Alquiler> buscarSolapamientos(@Param("espacioId") Long espacioId, @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin, @Param("idExcluido") Long idExcluido);

    @EntityGraph(attributePaths = {"cliente", "espacio"})
    @Query("""
            SELECT a FROM Alquiler a
            WHERE a.espacio.id = :espacioId
              AND a.estado <> com.uca.cfc.entity.enums.EstadoReserva.CANCELADA
              AND a.fechaHoraInicio < :fin
              AND a.fechaHoraFin > :inicio
            ORDER BY a.fechaHoraInicio
            """)
    List<Alquiler> findOcupacionEnRango(@Param("espacioId") Long espacioId, @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    boolean existsByEspacioId(Long espacioId);

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "espacio"})
    Optional<Alquiler> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = {"cliente", "espacio"})
    @Query("""
            SELECT a FROM Alquiler a
            WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId)
              AND (:espacioId IS NULL OR a.espacio.id = :espacioId)
              AND (:estado IS NULL OR a.estado = :estado)
              AND (CAST(:desde AS timestamp) IS NULL OR a.fechaHoraInicio >= :desde)
              AND (CAST(:hasta AS timestamp) IS NULL OR a.fechaHoraFin <= :hasta)
            """)
    Page<Alquiler> buscar(@Param("clienteId") Long clienteId, @Param("espacioId") Long espacioId,
            @Param("estado") EstadoReserva estado, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta, Pageable pageable);
}
