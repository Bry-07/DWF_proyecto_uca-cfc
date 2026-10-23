package com.uca.cfc.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.uca.cfc.entity.enums.EstadoInscripcion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Inscripción de un participante a un curso o a un diplomado.
 * Exactamente una de las dos referencias está presente; la base de datos lo
 * garantiza con un CHECK y la unicidad del participante se controla por oferta.
 */
@Entity
@Table(name = "inscripciones")
public class Inscripcion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diplomado_id")
    private Diplomado diplomado;

    /** Persona que asiste. Puede no ser el cliente cuando una empresa inscribe a su personal. */
    @Column(nullable = false, length = 150)
    private String participanteNombre;

    @Column(nullable = false, length = 150)
    private String participanteEmail;

    @Column(length = 20)
    private String participanteTelefono;

    @Column(nullable = false)
    private LocalDate fechaInscripcion;

    /** Precio de la oferta al momento de inscribir; no cambia si luego suben los precios. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado = EstadoInscripcion.PENDIENTE;

    @Column(length = 500)
    private String observaciones;

    public boolean esDeCurso() {
        return curso != null;
    }

    /** Nombre de la oferta inscrita, sea curso o diplomado. */
    public String getNombreOferta() {
        return esDeCurso() ? curso.getNombre() : diplomado.getNombre();
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public Diplomado getDiplomado() {
        return diplomado;
    }

    public void setDiplomado(Diplomado diplomado) {
        this.diplomado = diplomado;
    }

    public String getParticipanteNombre() {
        return participanteNombre;
    }

    public void setParticipanteNombre(String participanteNombre) {
        this.participanteNombre = participanteNombre;
    }

    public String getParticipanteEmail() {
        return participanteEmail;
    }

    public void setParticipanteEmail(String participanteEmail) {
        this.participanteEmail = participanteEmail;
    }

    public String getParticipanteTelefono() {
        return participanteTelefono;
    }

    public void setParticipanteTelefono(String participanteTelefono) {
        this.participanteTelefono = participanteTelefono;
    }

    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDate fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
