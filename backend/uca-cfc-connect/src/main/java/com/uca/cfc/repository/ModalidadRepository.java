package com.uca.cfc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.Modalidad;
import com.uca.cfc.entity.enums.TipoModalidad;

/**
 * Acceso a datos de las modalidades.
 */
public interface ModalidadRepository extends JpaRepository<Modalidad, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Modalidad> findByTipo(TipoModalidad tipo);
}
