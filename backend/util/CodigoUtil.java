package com.uca.cfc.util;

/**
 * Genera códigos legibles (PAG-0231, COT-0032...) a partir del id.
 * No se almacenan: se derivan del identificador, por lo que siempre son únicos.
 */
public final class CodigoUtil {

    private CodigoUtil() {
    }

    public static String formatear(String prefijo, Long id, int digitos) {
        if (id == null) {
            return null;
        }
        return prefijo + "-" + String.format("%0" + digitos + "d", id);
    }
}
