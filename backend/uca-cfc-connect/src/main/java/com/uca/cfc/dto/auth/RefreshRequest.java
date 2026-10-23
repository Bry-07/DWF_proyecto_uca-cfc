package com.uca.cfc.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "el refresh token es obligatorio") String refreshToken) {
}
