package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 409: ya existe un recurso con un valor que debe ser único. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
