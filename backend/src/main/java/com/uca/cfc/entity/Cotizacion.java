package com.uca.cfc.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.uca.cfc.entity.enums.EstadoCotizacion;
import com.uca.cfc.entity.enums.TipoCotizacion;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Cotizacion de servicios para un cliente.
 * El total no se manda desde afuera: se calcula sumando los subtotales de los
 * items. La lista de items se guarda y se borra junto con la cotizacion,
 * por eso lleva cascade y orphanRemoval.
 */
@Entity
@Table(name = "cotizaciones")
public class Cotizacion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Se calcula a partir de los ítems: si hay más de un tipo, es COMBINADA. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCotizacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    @Column(nullable = false, length = 255)
    private String asunto;

    @Column(nullable = false)
    private LocalDate fechaSolicitud;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 500)
    private String notaInterna;

    @Column(length = 150)
    private String responsableRevision;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CotizacionItem> items = new ArrayList<>();

    public void agregarItem(CotizacionItem item) {
        items.add(item);
        item.setCotizacion(this);
    }

    public void limpiarItems() {
        items.forEach(item -> item.setCotizacion(null));
        items.clear();
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoCotizacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoCotizacion tipo) {
        this.tipo = tipo;
    }

    public EstadoCotizacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoCotizacion estado) {
        this.estado = estado;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public LocalDate getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDate fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getNotaInterna() {
        return notaInterna;
    }

    public void setNotaInterna(String notaInterna) {
        this.notaInterna = notaInterna;
    }

    public String getResponsableRevision() {
        return responsableRevision;
    }

    public void setResponsableRevision(String responsableRevision) {
        this.responsableRevision = responsableRevision;
    }

    public List<CotizacionItem> getItems() {
        return items;
    }

    public void setItems(List<CotizacionItem> items) {
        this.items = items;
    }
}
