package com.uca.cfc.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;

/** 409: el espacio ya tiene una actividad que se superpone con el horario solicitado. */
public class EspacioOcupadoException extends ApiException {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EspacioOcupadoException(String espacio, String actividad, LocalDateTime inicio, LocalDateTime fin) {
        super(HttpStatus.CONFLICT,
                "El espacio '" + espacio + "' ya está ocupado para el horario solicitado. Conflicto con '"
                        + actividad + "' (" + FORMATO.format(inicio) + " – " + FORMATO.format(fin) + ")");
    }
}
