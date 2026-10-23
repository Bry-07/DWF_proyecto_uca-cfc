package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 401: correo o contraseña incorrectos, o un token que no sirve. */
public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
