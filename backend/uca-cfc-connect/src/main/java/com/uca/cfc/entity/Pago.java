package com.uca.cfc.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.EstadoPago;
import com.uca.cfc.entity.enums.MetodoPago;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Pago registrado por un cliente. Usa borrado lógico: {@code eliminado = true}
 * oculta el registro de todas las consultas JPA sin perder la trazabilidad contable.
 */
@Entity
@Table(name = "pagos")
@SQLRestriction("eliminado = false")
public class Pago extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Qué se está pagando: inscripción, cotización, alquiler o catering. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConceptoPago concepto;

    /** Id del registro al que corresponde el pago, según el concepto. */
    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEsperado;

    /** Suma efectivamente recibida. Determina si el pago es PARCIAL o PAGADO. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(nullable = false)
    private LocalDate fechaPago;

    @Column(length = 100)
    private String referenciaBancaria;

    /** Validación financiera (rol CONTABILIDAD). */
    @Column
    private LocalDateTime fechaValidacion;

    @Column(length = 100)
    private String validadoPor;

    /** Confirmación en recepción (rol RECEPCIONISTA). */
    @Column
    private LocalDateTime fechaConfirmacion;

    @Column(length = 100)
    private String confirmadoPor;

    @Column(nullable = false)
    private boolean eliminado = false;

    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Comprobante comprobante;

    /** Saldo pendiente de cobro. */
    public BigDecimal getSaldo() {
        return montoEsperado.subtract(montoPagado);
    }

    public boolean estaValidado() {
        return fechaValidacion != null;
    }

    public boolean estaConfirmado() {
        return fechaConfirmacion != null;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public ConceptoPago getConcepto() {
        return concepto;
    }

    public void setConcepto(ConceptoPago concepto) {
        this.concepto = concepto;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(Long referenciaId) {
        this.referenciaId = referenciaId;
    }

    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }

    public void setMontoEsperado(BigDecimal montoEsperado) {
        this.montoEsperado = montoEsperado;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getReferenciaBancaria() {
        return referenciaBancaria;
    }

    public void setReferenciaBancaria(String referenciaBancaria) {
        this.referenciaBancaria = referenciaBancaria;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public String getValidadoPor() {
        return validadoPor;
    }

    public void setValidadoPor(String validadoPor) {
        this.validadoPor = validadoPor;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public String getConfirmadoPor() {
        return confirmadoPor;
    }

    public void setConfirmadoPor(String confirmadoPor) {
        this.confirmadoPor = confirmadoPor;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public Comprobante getComprobante() {
        return comprobante;
    }

    public void setComprobante(Comprobante comprobante) {
        this.comprobante = comprobante;
        if (comprobante != null) {
            comprobante.setPago(this);
        }
    }
}
