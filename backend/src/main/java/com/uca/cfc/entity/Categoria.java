package com.uca.cfc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Categoria a la que pertenece un curso o diplomado (Tecnologia, Finanzas...).
 * Cada entidad representa una tabla de la base de datos: las anotaciones @Entity
 * y @Table le dicen a JPA a que tabla corresponde.
 */
@Entity
@Table(name = "categorias")
public class Categoria extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

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
