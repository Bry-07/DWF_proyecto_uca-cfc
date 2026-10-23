package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Curso;
import com.uca.cfc.entity.enums.EstadoOferta;

import jakarta.persistence.LockModeType;

/**
 * Acceso a datos de los cursos.
 * Usa @EntityGraph para traer de una sola vez la categoria, la modalidad, el
 * docente y el espacio, y asi evitar el problema de las N+1 consultas.
 * findForUpdateById bloquea la fila del curso para el control de cupos.
 */
public interface CursoRepository extends JpaRepository<Curso, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByModalidadId(Long modalidadId);

    boolean existsByDocenteId(Long docenteId);

    boolean existsByEspacioId(Long espacioId);

    /** Carga las relaciones en una sola consulta para evitar el problema N+1. */
    @EntityGraph(attributePaths = {"categoria", "modalidad", "docente", "espacio"})
    Optional<Curso> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = {"categoria", "modalidad", "docente", "espacio"})
    @Query("""
            SELECT c FROM Curso c
            WHERE (:categoriaId IS NULL OR c.categoria.id = :categoriaId)
              AND (:modalidadId IS NULL OR c.modalidad.id = :modalidadId)
              AND (:docenteId IS NULL OR c.docente.id = :docenteId)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (CAST(:texto AS String) IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:texto AS String), '%'))
                   OR LOWER(c.codigo) LIKE LOWER(CONCAT('%', CAST(:texto AS String), '%')))
            """)
    Page<Curso> buscar(@Param("categoriaId") Long categoriaId, @Param("modalidadId") Long modalidadId,
            @Param("docenteId") Long docenteId, @Param("estado") EstadoOferta estado,
            @Param("texto") String texto, Pageable pageable);

    /**
     * Bloquea la fila del curso hasta el final de la transacción. Evita que dos
     * inscripciones simultáneas superen el cupo máximo al leer el mismo conteo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Curso c WHERE c.id = :id")
    Optional<Curso> findForUpdateById(@Param("id") Long id);
}
