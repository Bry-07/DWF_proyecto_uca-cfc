package com.uca.cfc.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.Categoria;

/**
 * Acceso a datos de las categorias.
 * Al extender JpaRepository ya vienen hechos los metodos basicos (guardar,
 * buscar por id, listar, borrar); aqui solo se agregan las busquedas propias.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Categoria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
