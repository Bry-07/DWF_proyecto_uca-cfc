package com.uca.cfc.security;

/** Nombres de claims reservados por la API para la identidad y el rol. */
public final class JwtClaims {

    public static final String EMISOR = "uca-cfc-connect";
    public static final String USUARIO_ID = "uid";
    public static final String ROL = "rol";

    private JwtClaims() {
    }
}
