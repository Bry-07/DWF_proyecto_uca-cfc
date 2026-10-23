package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso solicitado no existe. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String recurso, Object id) {
        super(HttpStatus.NOT_FOUND, recurso + " con id " + id + " no encontrado");
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
