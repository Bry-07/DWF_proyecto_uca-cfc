package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 409: el recurso no puede eliminarse porque otros registros dependen de él. */
public class ResourceInUseException extends ApiException {

    public ResourceInUseException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
