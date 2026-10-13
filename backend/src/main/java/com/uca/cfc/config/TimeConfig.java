package com.uca.cfc.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación. Inyectarlo (en lugar de llamar a LocalDateTime.now())
 * permite fijar la fecha en las pruebas unitarias.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.zona-horaria:America/El_Salvador}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
