package com.uca.cfc.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.uca.cfc.entity.enums.TipoItemCotizacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Línea de una cotización. La referencia al recurso (curso, espacio, servicio)
 * es opcional: permite cotizar algo que aún no existe en el catálogo.
 */
@Entity
@Table(name = "cotizacion_items")
public class CotizacionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItemCotizacion tipo;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Column(length = 255)
    private String detalle;

    /** Id del curso, diplomado, espacio o servicio de catering cotizado, si aplica. */
    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Recalcula el subtotal a partir de cantidad y precio unitario. */
    public void calcularSubtotal() {
        if (cantidad == null || precioUnitario == null) {
            subtotal = BigDecimal.ZERO;
            return;
        }
        subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public TipoItemCotizacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoItemCotizacion tipo) {
        this.tipo = tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(Long referenciaId) {
        this.referenciaId = referenciaId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
