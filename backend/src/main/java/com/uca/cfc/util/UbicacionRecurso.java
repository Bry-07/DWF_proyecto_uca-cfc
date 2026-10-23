package com.uca.cfc.util;

import java.net.URI;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Construye el header Location de las respuestas 201 Created. */
public final class UbicacionRecurso {

    private UbicacionRecurso() {
    }

    public static URI de(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri();
    }
}
