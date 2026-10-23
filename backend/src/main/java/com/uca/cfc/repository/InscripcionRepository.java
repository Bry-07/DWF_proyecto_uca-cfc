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

import com.uca.cfc.entity.Inscripcion;
import com.uca.cfc.entity.enums.EstadoInscripcion;

/**
 * Acceso a datos de las inscripciones.
 * Aqui estan las consultas que sostienen la regla del cupo:
 * contarOcupadasPorCurso cuenta las inscripciones que ocupan lugar (todas
 * menos las canceladas) y existeParticipanteEnCurso revisa que la misma
 * persona no se inscriba dos veces a la misma oferta.
 */
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    /**
     * Cuenta las inscripciones que ocupan cupo (todas menos las canceladas).
     * Es la consulta que respalda la validación de cupo máximo.
     */
    @Query("""
            SELECT COUNT(i) FROM Inscripcion i
            WHERE i.curso.id = :cursoId AND i.estado <> com.uca.cfc.entity.enums.EstadoInscripcion.CANCELADA
            """)
    long contarOcupadasPorCurso(@Param("cursoId") Long cursoId);

    @Query("""
            SELECT COUNT(i) FROM Inscripcion i
            WHERE i.diplomado.id = :diplomadoId
              AND i.estado <> com.uca.cfc.entity.enums.EstadoInscripcion.CANCELADA
            """)
    long contarOcupadasPorDiplomado(@Param("diplomadoId") Long diplomadoId);

    @Query("""
            SELECT COUNT(i) > 0 FROM Inscripcion i
            WHERE i.curso.id = :cursoId
              AND LOWER(i.participanteEmail) = LOWER(:email)
              AND i.estado <> com.uca.cfc.entity.enums.EstadoInscripcion.CANCELADA
              AND (:idExcluido IS NULL OR i.id <> :idExcluido)
            """)
    boolean existeParticipanteEnCurso(@Param("cursoId") Long cursoId, @Param("email") String email,
            @Param("idExcluido") Long idExcluido);

    @Query("""
            SELECT COUNT(i) > 0 FROM Inscripcion i
            WHERE i.diplomado.id = :diplomadoId
              AND LOWER(i.participanteEmail) = LOWER(:email)
              AND i.estado <> com.uca.cfc.entity.enums.EstadoInscripcion.CANCELADA
              AND (:idExcluido IS NULL OR i.id <> :idExcluido)
            """)
    boolean existeParticipanteEnDiplomado(@Param("diplomadoId") Long diplomadoId, @Param("email") String email,
            @Param("idExcluido") Long idExcluido);

    boolean existsByCursoId(Long cursoId);

    boolean existsByDiplomadoId(Long diplomadoId);

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "curso", "diplomado"})
    Optional<Inscripcion> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = {"cliente", "curso", "diplomado"})
    List<Inscripcion> findByClienteIdOrderByFechaInscripcionDesc(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "curso", "diplomado"})
    @Query("""
            SELECT i FROM Inscripcion i
            WHERE (:clienteId IS NULL OR i.cliente.id = :clienteId)
              AND (:cursoId IS NULL OR i.curso.id = :cursoId)
              AND (:diplomadoId IS NULL OR i.diplomado.id = :diplomadoId)
              AND (:estado IS NULL OR i.estado = :estado)
              AND (CAST(:desde AS date) IS NULL OR i.fechaInscripcion >= :desde)
              AND (CAST(:hasta AS date) IS NULL OR i.fechaInscripcion <= :hasta)
            """)
    Page<Inscripcion> buscar(@Param("clienteId") Long clienteId, @Param("cursoId") Long cursoId,
            @Param("diplomadoId") Long diplomadoId, @Param("estado") EstadoInscripcion estado,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, Pageable pageable);
}
