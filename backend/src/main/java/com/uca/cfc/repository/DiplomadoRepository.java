package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Diplomado;
import com.uca.cfc.entity.enums.EstadoOferta;

import jakarta.persistence.LockModeType;

/**
 * Acceso a datos de los diplomados. Funciona igual que CursoRepository.
 */
public interface DiplomadoRepository extends JpaRepository<Diplomado, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByModalidadId(Long modalidadId);

    boolean existsByDocenteId(Long docenteId);

    boolean existsByEspacioId(Long espacioId);

    @EntityGraph(attributePaths = {"categoria", "modalidad", "docente", "espacio"})
    Optional<Diplomado> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = {"categoria", "modalidad", "docente", "espacio"})
    @Query("""
            SELECT d FROM Diplomado d
            WHERE (:categoriaId IS NULL OR d.categoria.id = :categoriaId)
              AND (:modalidadId IS NULL OR d.modalidad.id = :modalidadId)
              AND (:docenteId IS NULL OR d.docente.id = :docenteId)
              AND (:estado IS NULL OR d.estado = :estado)
              AND (CAST(:texto AS String) IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', CAST(:texto AS String), '%'))
                   OR LOWER(d.codigo) LIKE LOWER(CONCAT('%', CAST(:texto AS String), '%')))
            """)
    Page<Diplomado> buscar(@Param("categoriaId") Long categoriaId, @Param("modalidadId") Long modalidadId,
            @Param("docenteId") Long docenteId, @Param("estado") EstadoOferta estado,
            @Param("texto") String texto, Pageable pageable);

    /** Bloqueo pesimista para el control de cupos (ver CursoRepository). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Diplomado d WHERE d.id = :id")
    Optional<Diplomado> findForUpdateById(@Param("id") Long id);
}
