package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uca.cfc.entity.Espacio;
import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.TipoEspacio;

import jakarta.persistence.LockModeType;

/**
 * Acceso a datos de los espacios.
 * Tiene un metodo especial, findForUpdateById, que bloquea la fila del espacio
 * mientras dura la transaccion. Sirve para que dos reservas al mismo tiempo no
 * puedan pasar la validacion de horario a la vez.
 */
public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
            SELECT e FROM Espacio e
            WHERE (:tipo IS NULL OR e.tipo = :tipo)
              AND (:estado IS NULL OR e.estado = :estado)
              AND (:capacidadMinima IS NULL OR e.capacidad >= :capacidadMinima)
              AND (CAST(:nombre AS String) IS NULL OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS String), '%')))
            """)
    Page<Espacio> buscar(@Param("tipo") TipoEspacio tipo, @Param("estado") EstadoEspacio estado,
            @Param("capacidadMinima") Integer capacidadMinima, @Param("nombre") String nombre, Pageable pageable);

    /**
     * Bloquea la fila del espacio hasta el final de la transacción, de modo que
     * dos reservas simultáneas no puedan pasar la validación de traslape a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Espacio e WHERE e.id = :id")
    Optional<Espacio> findForUpdateById(@Param("id") Long id);
}
