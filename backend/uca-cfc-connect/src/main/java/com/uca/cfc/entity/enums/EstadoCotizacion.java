package com.uca.cfc.entity.enums;

/**
 * PENDIENTE: tiene al menos un ítem sin precio. EN_PROCESO: todos los ítems están valorizados.
 * APROBADA y RECHAZADA son estados finales.
 */
public enum EstadoCotizacion {
    PENDIENTE, EN_PROCESO, APROBADA, RECHAZADA;

    public boolean esEditable() {
        return this == PENDIENTE || this == EN_PROCESO;
    }
}
