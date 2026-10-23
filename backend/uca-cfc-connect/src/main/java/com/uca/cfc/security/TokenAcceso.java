package com.uca.cfc.security;

import java.time.Instant;

/** Access token ya firmado, junto con los datos que el servicio necesita de él. */
public record TokenAcceso(String valor, String jti, Instant expiracion, long segundosDeVida) {
}
