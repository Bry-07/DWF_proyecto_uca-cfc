package com.uca.cfc.entity.enums;

/**
 * PENDIENTE: tiene al menos un ítem sin precio. GENERADA: todos los ítems están valorizados.
 * APROBADA y RECHAZADA son estados finales.
 */
public enum EstadoCotizacion {
    PENDIENTE, GENERADA, APROBADA, RECHAZADA;

    public boolean esEditable() {
        return this == PENDIENTE || this == GENERADA;
    }
}
