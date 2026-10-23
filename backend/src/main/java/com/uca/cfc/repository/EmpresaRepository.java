package com.uca.cfc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uca.cfc.entity.Empresa;

/**
 * Acceso a datos de las empresas.
 * No tiene consultas propias porque a una empresa siempre se llega desde su cliente.
 */
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
