package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/**
 * Base de las excepciones de negocio. Cada subclase define su código HTTP,
 * así el manejador global no necesita un método por excepción.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
