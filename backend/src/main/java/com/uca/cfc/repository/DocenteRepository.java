package com.uca.cfc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Docente;

/**
 * Acceso a datos de los docentes.
 * La consulta buscar() tiene los filtros opcionales: si un parametro llega
 * nulo, esa condicion se ignora y no filtra por ella.
 */
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            SELECT d FROM Docente d
            WHERE (CAST(:especialidad AS String) IS NULL OR LOWER(d.especialidad) LIKE LOWER(CONCAT('%', CAST(:especialidad AS String), '%')))
              AND (CAST(:nombre AS String) IS NULL OR LOWER(CONCAT(d.nombres, ' ', d.apellidos)) LIKE LOWER(CONCAT('%', CAST(:nombre AS String), '%')))
            """)
    Page<Docente> buscar(@Param("especialidad") String especialidad, @Param("nombre") String nombre,
            Pageable pageable);
}
