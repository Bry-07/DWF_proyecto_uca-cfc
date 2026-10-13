package com.uca.cfc.entity.enums;

/**
 * Tipo de evento de la agenda institucional. ALQUILER y CATERING se generan
 * automáticamente desde sus módulos; el resto se registra en /agenda.
 */
public enum TipoActividad {
    CURSO, DIPLOMADO, ALQUILER, CATERING, EVENTO;

    public boolean esSincronizado() {
        return this == ALQUILER || this == CATERING;
    }
}
