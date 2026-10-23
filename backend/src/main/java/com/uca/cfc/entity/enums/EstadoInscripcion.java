package com.uca.cfc.entity.enums;

/**
 * Flujo: PENDIENTE → CONFIRMADA → FINALIZADA. PENDIENTE y CONFIRMADA pueden pasar a CANCELADA.
 * CANCELADA y FINALIZADA son estados finales.
 */
public enum EstadoInscripcion {
    PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA;

    public boolean puedeCambiarA(EstadoInscripcion destino) {
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
