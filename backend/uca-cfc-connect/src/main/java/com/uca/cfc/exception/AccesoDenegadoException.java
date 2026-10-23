package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 403: el usuario está autenticado pero no puede hacer esto o ver este recurso. */
public class AccesoDenegadoException extends ApiException {

    public AccesoDenegadoException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
