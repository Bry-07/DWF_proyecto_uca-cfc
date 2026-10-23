package com.uca.cfc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.ServicioCatering;
import com.uca.cfc.entity.enums.TipoServicioCatering;

/**
 * Acceso a datos del catalogo de servicios de catering.
 */
public interface ServicioCateringRepository extends JpaRepository<ServicioCatering, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
            SELECT s FROM ServicioCatering s
            WHERE (:tipo IS NULL OR s.tipo = :tipo)
              AND (:activo IS NULL OR s.activo = :activo)
            """)
    Page<ServicioCatering> buscar(@Param("tipo") TipoServicioCatering tipo, @Param("activo") Boolean activo,
            Pageable pageable);
}
