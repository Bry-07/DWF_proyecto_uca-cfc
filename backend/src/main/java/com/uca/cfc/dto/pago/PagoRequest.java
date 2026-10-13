package com.uca.cfc.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.uca.cfc.entity.enums.ConceptoPago;
import com.uca.cfc.entity.enums.MetodoPago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un pago.
 * El concepto y la referencia dicen que se esta pagando: por ejemplo
 * concepto INSCRIPCION y referenciaId 5 significa la inscripcion numero 5.
 */
public record PagoRequest(
        @NotNull(message = "el cliente es obligatorio")
        @Positive(message = "el id del cliente debe ser positivo")
        @Schema(example = "1") Long clienteId,

        @NotNull(message = "el concepto es obligatorio")
        @Schema(example = "INSCRIPCION") ConceptoPago concepto,

        @NotNull(message = "la referencia del concepto es obligatoria")
        @Positive(message = "el id de referencia debe ser positivo")
        @Schema(description = "Id de la inscripción, cotización, alquiler o solicitud de catering", example = "1")
        Long referenciaId,

        @NotNull(message = "el monto esperado es obligatorio")
        @Positive(message = "el monto esperado debe ser mayor que 0")
        @DecimalMax(value = "999999999.99", message = "el monto excede el máximo permitido")
        @Schema(example = "185.00") BigDecimal montoEsperado,

        @PositiveOrZero(message = "el monto pagado no puede ser negativo")
        @DecimalMax(value = "999999999.99", message = "el monto excede el máximo permitido")
        @Schema(description = "Monto recibido al registrar el pago. Si se omite, el pago queda PENDIENTE",
                example = "185.00") BigDecimal montoPagado,

        @NotNull(message = "el método de pago es obligatorio")
        @Schema(example = "TARJETA") MetodoPago metodo,

        @Schema(description = "Si se omite, se usa la fecha actual", example = "2026-09-11") LocalDate fechaPago,

        @Size(max = 100, message = "la referencia bancaria no puede exceder 100 caracteres")
        @Schema(example = "AUT-884512") String referenciaBancaria) {
}
