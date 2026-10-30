package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

public class CorreoNoDisponibleException extends ApiException {
    public CorreoNoDisponibleException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de correo no está disponible. Intente más tarde");
    }
}
