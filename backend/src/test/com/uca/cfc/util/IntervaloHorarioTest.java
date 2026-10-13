package com.uca.cfc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.uca.cfc.exception.BusinessException;

/**
 * La regla de solapamiento es el corazón de la reserva de espacios, así que se
 * prueba aislada de la base de datos y de Spring.
 */
@DisplayName("IntervaloHorario")
class IntervaloHorarioTest {

    private static IntervaloHorario intervalo(String inicio, String fin) {
        return new IntervaloHorario(LocalDateTime.parse(inicio), LocalDateTime.parse(fin));
    }

    @Nested
    @DisplayName("detección de traslapes")
    class Traslapes {

        private final IntervaloHorario reserva = intervalo("2026-09-20T08:00:00", "2026-09-20T11:00:00");

        @Test
        @DisplayName("el mismo horario se traslapa")
        void mismoHorario() {
            assertTrue(reserva.seSolapaCon(intervalo("2026-09-20T08:00:00", "2026-09-20T11:00:00")));
        }

        @Test
        @DisplayName("un horario que empieza dentro se traslapa")
        void empiezaDentro() {
            assertTrue(reserva.seSolapaCon(intervalo("2026-09-20T10:00:00", "2026-09-20T12:00:00")));
        }

        @Test
        @DisplayName("un horario que termina dentro se traslapa")
        void terminaDentro() {
            assertTrue(reserva.seSolapaCon(intervalo("2026-09-20T07:00:00", "2026-09-20T09:00:00")));
        }

        @Test
        @DisplayName("un horario que contiene por completo al otro se traslapa")
        void contieneAlOtro() {
            assertTrue(reserva.seSolapaCon(intervalo("2026-09-20T07:00:00", "2026-09-20T13:00:00")));
        }

        @Test
        @DisplayName("un horario contenido dentro del otro se traslapa")
        void contenidoEnElOtro() {
            assertTrue(reserva.seSolapaCon(intervalo("2026-09-20T09:00:00", "2026-09-20T10:00:00")));
        }

        @Test
        @DisplayName("un horario que empieza justo cuando el otro termina NO se traslapa")
        void contiguoDespues() {
            assertFalse(reserva.seSolapaCon(intervalo("2026-09-20T11:00:00", "2026-09-20T13:00:00")));
        }

        @Test
        @DisplayName("un horario que termina justo cuando el otro empieza NO se traslapa")
        void contiguoAntes() {
            assertFalse(reserva.seSolapaCon(intervalo("2026-09-20T06:00:00", "2026-09-20T08:00:00")));
        }

        @Test
        @DisplayName("un horario en otro día NO se traslapa")
        void otroDia() {
            assertFalse(reserva.seSolapaCon(intervalo("2026-09-21T08:00:00", "2026-09-21T11:00:00")));
        }
    }

    @Test
    @DisplayName("rechaza un intervalo cuyo fin no es posterior al inicio")
    void finAntesDelInicio() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> intervalo("2026-09-20T11:00:00", "2026-09-20T08:00:00"));
        assertTrue(ex.getMessage().contains("posterior"));
    }

    @Test
    @DisplayName("rechaza un intervalo de duración cero")
    void duracionCero() {
        assertThrows(BusinessException.class, () -> intervalo("2026-09-20T08:00:00", "2026-09-20T08:00:00"));
    }

    @Test
    @DisplayName("calcula la duración en minutos")
    void duracion() {
        assertEquals(90, intervalo("2026-09-20T08:00:00", "2026-09-20T09:30:00").duracion().toMinutes());
    }
}
