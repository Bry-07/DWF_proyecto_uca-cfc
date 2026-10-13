package com.uca.cfc.entity.enums;

/** Tipo de cada línea de una cotización. Una cotización con varios tipos es COMBINADA. */
public enum TipoItemCotizacion {
    CURSO, DIPLOMADO, ESPACIO, CATERING;

    public TipoCotizacion comoTipoCotizacion() {
        return TipoCotizacion.valueOf(name());
    }
}
