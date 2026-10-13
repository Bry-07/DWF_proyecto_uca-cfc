package com.uca.cfc.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.uca.cfc.exception.BusinessException;

/**
 * Rango de tiempo [inicio, fin). El fin es exclusivo: una actividad de 8:00 a 10:00
 * no choca con otra que empieza a las 10:00.
 */
public record IntervaloHorario(LocalDateTime inicio, LocalDateTime fin) {

    public IntervaloHorario {
        if (inicio == null || fin == null) {
            throw new BusinessException("Debe indicar la fecha y hora de inicio y de fin");
        }
        if (!fin.isAfter(inicio)) {
            throw new BusinessException("La fecha y hora de fin debe ser posterior a la de inicio");
        }
    }

    public static IntervaloHorario of(LocalDate fechaInicio, LocalTime horaInicio, LocalDate fechaFin,
            LocalTime horaFin) {
        return new IntervaloHorario(LocalDateTime.of(fechaInicio, horaInicio), LocalDateTime.of(fechaFin, horaFin));
    }

    /**
     * Regla de solapamiento: nuevoInicio &lt; existenteFin AND nuevoFin &gt; existenteInicio.
     */
    public boolean seSolapaCon(IntervaloHorario otro) {
        return inicio.isBefore(otro.fin) && fin.isAfter(otro.inicio);
    }

    public Duration duracion() {
        return Duration.between(inicio, fin);
    }
}
