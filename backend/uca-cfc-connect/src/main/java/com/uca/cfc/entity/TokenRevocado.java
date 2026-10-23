package com.uca.cfc.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Access token al que se le cerró la sesión. Mientras no venza, el filtro de
 * seguridad lo rechaza aunque la firma siga siendo válida.
 */
@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
