package com.uca.cfc.entity;

import com.uca.cfc.entity.enums.TipoModalidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Modalidad en que se imparte una oferta: presencial, virtual o hibrida.
 */
@Entity
@Table(name = "modalidades")
public class Modalidad extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoModalidad tipo;

    @Column(length = 255)
    private String descripcion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoModalidad getTipo() {
        return tipo;
    }

    public void setTipo(TipoModalidad tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
