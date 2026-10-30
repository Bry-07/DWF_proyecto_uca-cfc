package com.uca.cfc.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL serializa el incremento incluso cuando los pagos son diferentes. */
@Service
public class NumeroComprobanteService {
    private final JdbcTemplate jdbc;

    public NumeroComprobanteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String siguiente(int anio) {
        Long numero = jdbc.queryForObject("""
                INSERT INTO comprobante_correlativos (anio, ultimo) VALUES (?, 1)
                ON CONFLICT (anio) DO UPDATE SET ultimo = comprobante_correlativos.ultimo + 1
                RETURNING ultimo
                """, Long.class, anio);
        return "CFC-" + anio + "-" + String.format("%06d", numero);
    }
}
