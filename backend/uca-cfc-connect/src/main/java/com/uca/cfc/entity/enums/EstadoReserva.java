package com.uca.cfc.entity.enums;

/**
 * Estado de alquileres y solicitudes de catering.
 * Flujo: PENDIENTE → CONFIRMADA → FINALIZADA; PENDIENTE y CONFIRMADA pueden pasar a CANCELADA.
 */
public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA;

    public boolean puedeCambiarA(EstadoReserva destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == FINALIZADA || destino == CANCELADA;
            case CANCELADA, FINALIZADA -> false;
        };
    }

    public boolean esFinal() {
        return this == CANCELADA || this == FINALIZADA;
    }
}
