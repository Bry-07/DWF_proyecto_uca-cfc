package com.uca.cfc.dto.pago;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Registro de un abono adicional sobre un pago existente. */
public record AbonoPagoRequest(
        @NotNull(message = "el monto del abono es obligatorio")
        @Positive(message = "el monto del abono debe ser mayor que 0")
        @DecimalMax(value = "999999999.99", message = "el monto excede el máximo permitido")
        @Schema(example = "1000.00") BigDecimal monto,

        @Size(max = 100, message = "la referencia bancaria no puede exceder 100 caracteres")
        @Schema(example = "TRF-2026-0910") String referenciaBancaria) {
}
