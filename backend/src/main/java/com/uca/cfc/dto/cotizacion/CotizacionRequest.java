package com.uca.cfc.dto.cotizacion;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * El tipo de la cotización no se envía: se deduce de los ítems.
 * Con un solo tipo de ítem toma ese tipo; con varios, es COMBINADA.
 */
public record CotizacionRequest(
        @NotNull(message = "el cliente es obligatorio")
        @Positive(message = "el id del cliente debe ser positivo")
        @Schema(example = "2") Long clienteId,

        @NotBlank(message = "el asunto es obligatorio")
        @Size(max = 255, message = "el asunto no puede exceder 255 caracteres")
        @Schema(example = "Evento institucional de cierre de año") String asunto,

        @Schema(description = "Si se omite, se usa la fecha actual", example = "2026-09-11")
        LocalDate fechaSolicitud,

        @Schema(description = "Si se omite, vence 30 días después de la solicitud", example = "2026-10-11")
        LocalDate fechaVencimiento,

        @Size(max = 500, message = "la nota interna no puede exceder 500 caracteres")
        @Schema(example = "Verificar disponibilidad del auditorio antes de aprobar.") String notaInterna,

        @Size(max = 150, message = "el responsable no puede exceder 150 caracteres")
        @Schema(example = "Ing. Yesenia Escobar — Administración") String responsableRevision,

        @NotEmpty(message = "la cotización debe tener al menos un ítem")
        @Size(max = 50, message = "la cotización no puede tener más de 50 ítems")
        @Valid List<CotizacionItemRequest> items) {
}
