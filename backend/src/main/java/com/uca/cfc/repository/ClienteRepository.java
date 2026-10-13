package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Cliente;
import com.uca.cfc.entity.enums.TipoCliente;

/**
 * Acceso a datos de los clientes.
 * La consulta usa LEFT JOIN con empresa para que el filtro por razon social no
 * deje fuera a las personas particulares, que no tienen empresa asociada.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByDui(String dui);

    boolean existsByDuiAndIdNot(String dui, Long id);

    boolean existsByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Long id);

    @EntityGraph(attributePaths = {"empresa"})
    Optional<Cliente> findWithEmpresaById(Long id);

    @EntityGraph(attributePaths = {"empresa"})
    @Query("""
            SELECT c FROM Cliente c
            LEFT JOIN c.empresa e
            WHERE (CAST(:nombre AS String) IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS String), '%')))
              AND (CAST(:dui AS String) IS NULL OR c.dui = CAST(:dui AS String))
              AND (CAST(:nit AS String) IS NULL OR c.nit = CAST(:nit AS String))
              AND (:tipo IS NULL OR c.tipo = :tipo)
              AND (CAST(:empresa AS String) IS NULL OR LOWER(e.razonSocial) LIKE LOWER(CONCAT('%', CAST(:empresa AS String), '%')))
            """)
    Page<Cliente> buscar(@Param("nombre") String nombre, @Param("dui") String dui, @Param("nit") String nit,
            @Param("tipo") TipoCliente tipo, @Param("empresa") String empresa, Pageable pageable);
}
