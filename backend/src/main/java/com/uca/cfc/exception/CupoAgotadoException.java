package com.uca.cfc.exception;

import org.springframework.http.HttpStatus;

/** 409: el curso o diplomado alcanzó su cupo máximo. */
public class CupoAgotadoException extends ApiException {

    public CupoAgotadoException(String oferta, int cupoMaximo) {
        super(HttpStatus.CONFLICT,
                "No hay cupos disponibles en '" + oferta + "': se alcanzó el cupo máximo de " + cupoMaximo);
    }
}
