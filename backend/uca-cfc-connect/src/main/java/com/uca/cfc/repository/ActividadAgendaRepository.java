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

import com.uca.cfc.entity.ActividadAgenda;
import com.uca.cfc.entity.enums.TipoActividad;

/**
 * Acceso a datos de la agenda institucional.
 * findParaAnalisisDeConflictos devuelve las actividades ordenadas por espacio y
 * por hora de inicio; asi el servicio puede recorrerlas comparando cada una con
 * la siguiente para detectar los traslapes.
 */
public interface ActividadAgendaRepository extends JpaRepository<ActividadAgenda, Long> {

    @EntityGraph(attributePaths = {"espacio"})
    Optional<ActividadAgenda> findWithEspacioById(Long id);

    Optional<ActividadAgenda> findByTipoAndOrigenId(TipoActividad tipo, Long origenId);

    boolean existsByEspacioId(Long espacioId);

    @EntityGraph(attributePaths = {"espacio"})
    @Query("""
            SELECT a FROM ActividadAgenda a
            WHERE (:tipo IS NULL OR a.tipo = :tipo)
              AND (:espacioId IS NULL OR a.espacio.id = :espacioId)
              AND (CAST(:desde AS timestamp) IS NULL OR a.fechaHoraFin >= :desde)
              AND (CAST(:hasta AS timestamp) IS NULL OR a.fechaHoraInicio <= :hasta)
            """)
    Page<ActividadAgenda> buscar(@Param("tipo") TipoActividad tipo, @Param("espacioId") Long espacioId,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable);

    /** Actividades de un espacio que se superponen con el rango indicado. */
    @EntityGraph(attributePaths = {"espacio"})
    @Query("""
            SELECT a FROM ActividadAgenda a
            WHERE a.espacio.id = :espacioId
              AND a.fechaHoraInicio < :fin
              AND a.fechaHoraFin > :inicio
              AND (:idExcluido IS NULL OR a.id <> :idExcluido)
            ORDER BY a.fechaHoraInicio
            """)
    List<ActividadAgenda> buscarSolapamientos(@Param("espacioId") Long espacioId,
            @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
            @Param("idExcluido") Long idExcluido);

    /**
     * Todas las actividades con espacio asignado dentro de un rango, ordenadas para
     * poder detectar conflictos comparando cada actividad con la siguiente.
     */
    @EntityGraph(attributePaths = {"espacio"})
    @Query("""
            SELECT a FROM ActividadAgenda a
            WHERE a.espacio IS NOT NULL
              AND (CAST(:desde AS timestamp) IS NULL OR a.fechaHoraFin >= :desde)
              AND (CAST(:hasta AS timestamp) IS NULL OR a.fechaHoraInicio <= :hasta)
            ORDER BY a.espacio.id, a.fechaHoraInicio
            """)
    List<ActividadAgenda> findParaAnalisisDeConflictos(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
