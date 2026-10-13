package com.uca.cfc.entity.enums;

/** Roles base definidos por la especificación. No pueden renombrarse ni eliminarse. */
public enum RolSistema {
    ADMIN, RECEPCIONISTA, CLIENTE, CONTABILIDAD;

    public static boolean esRolSistema(String nombre) {
        for (RolSistema rol : values()) {
            if (rol.name().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }
}
