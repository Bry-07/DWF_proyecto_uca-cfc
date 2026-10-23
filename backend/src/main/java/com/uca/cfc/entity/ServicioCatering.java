package com.uca.cfc.entity;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.TipoServicioCatering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Servicio de catering ofrecido por el centro (coffee break, almuerzo, etc.). */
@Entity
@Table(name = "servicios_catering")
public class ServicioCatering extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoServicioCatering tipo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPorPersona;

    @Column(nullable = false)
    private Integer minimoPersonas;

    @Column(nullable = false)
    private boolean activo = true;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoServicioCatering getTipo() {
        return tipo;
    }

    public void setTipo(TipoServicioCatering tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioPorPersona() {
        return precioPorPersona;
    }

    public void setPrecioPorPersona(BigDecimal precioPorPersona) {
        this.precioPorPersona = precioPorPersona;
    }

    public Integer getMinimoPersonas() {
        return minimoPersonas;
    }

    public void setMinimoPersonas(Integer minimoPersonas) {
        this.minimoPersonas = minimoPersonas;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
