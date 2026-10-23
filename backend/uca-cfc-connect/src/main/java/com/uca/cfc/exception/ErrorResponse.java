package com.uca.cfc.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Formato unico de respuesta para todos los errores de la API.
 * La lista de errores solo se llena cuando falla la validacion de campos;
 * en los demas casos va vacia.
 */
@Schema(description = "Formato estándar de error de la API")
public record ErrorResponse(
        @Schema(example = "2026-09-11T10:15:30") LocalDateTime timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "El espacio 'Aula 3' ya está ocupado para el horario solicitado") String message,
        @Schema(example = "/api/v1/alquileres") String path,
        @Schema(description = "Detalle por campo (solo en errores de validación)")
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<CampoError> errores) {

    public record CampoError(
            @Schema(example = "cupoMaximo") String campo,
            @Schema(example = "debe ser mayor que 0") String mensaje) {
    }
}
