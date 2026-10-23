package com.uca.cfc.entity;

import com.uca.cfc.entity.enums.RolSistema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Rol de acceso al sistema.
 * Los cuatro roles base (ADMIN, RECEPCIONISTA, CLIENTE, CONTABILIDAD) estan
 * protegidos para que no se puedan renombrar ni borrar.
 */
@Entity
@Table(name = "roles")
public class Rol extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    public boolean esDelSistema() {
        return RolSistema.esRolSistema(nombre);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
