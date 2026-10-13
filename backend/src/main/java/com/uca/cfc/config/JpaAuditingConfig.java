package com.uca.cfc.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa la auditoría de Spring Data (createdAt, updatedAt, createdBy, updatedBy).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    public static final String AUDITOR_SISTEMA = "sistema";

    /**
     * Mientras no exista autenticación, las operaciones se atribuyen a "sistema".
     * En la Fase 3 este bean leerá el usuario autenticado desde el SecurityContext.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(AUDITOR_SISTEMA);
    }
}
