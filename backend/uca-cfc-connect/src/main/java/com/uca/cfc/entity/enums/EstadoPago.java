package com.uca.cfc.entity.enums;

/**
 * Flujo financiero hacia adelante: PENDIENTE → PARCIAL → PAGADO (o PENDIENTE → PAGADO).
 * Nunca se retrocede y PAGADO es final.
 */
public enum EstadoPago {
    PENDIENTE, PARCIAL, PAGADO;

    public boolean puedeCambiarA(EstadoPago destino) {
        return switch (this) {
            case PENDIENTE -> destino == PARCIAL || destino == PAGADO;
            case PARCIAL -> destino == PAGADO;
            case PAGADO -> false;
        };
    }
}
