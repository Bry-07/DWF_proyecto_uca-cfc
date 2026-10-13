package com.uca.cfc.entity;

import java.math.BigDecimal;

import com.uca.cfc.entity.enums.EstadoEspacio;
import com.uca.cfc.entity.enums.TipoEspacio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Espacio fisico del centro: auditorio, aula, laboratorio o sala.
 * Guarda la capacidad y el precio por hora, que son los datos con los que
 * despues se valida y se cobra un alquiler.
 */
@Entity
@Table(name = "espacios")
public class Espacio extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPorHora;

    @Column(length = 500)
    private String equipamiento;

    @Column(length = 150)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEspacio estado = EstadoEspacio.DISPONIBLE;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoEspacio getTipo() {
        return tipo;
    }

    public void setTipo(TipoEspacio tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getPrecioPorHora() {
        return precioPorHora;
    }

    public void setPrecioPorHora(BigDecimal precioPorHora) {
        this.precioPorHora = precioPorHora;
    }

    public String getEquipamiento() {
        return equipamiento;
    }

    public void setEquipamiento(String equipamiento) {
        this.equipamiento = equipamiento;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public EstadoEspacio getEstado() {
        return estado;
    }

    public void setEstado(EstadoEspacio estado) {
        this.estado = estado;
    }
}
