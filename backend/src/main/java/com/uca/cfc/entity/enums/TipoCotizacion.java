package com.uca.cfc.entity.enums;

/**
 * Tipo de cotizacion. No se envia desde afuera: el servicio lo deduce mirando
 * los items. Si hay items de varios tipos, la cotizacion queda como COMBINADA.
 */
public enum TipoCotizacion {
    CURSO, DIPLOMADO, ESPACIO, CATERING, COMBINADA
}
