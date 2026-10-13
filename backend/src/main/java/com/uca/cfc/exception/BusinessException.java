package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 422: la solicitud es válida en formato, pero viola una regla de negocio. */
public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
