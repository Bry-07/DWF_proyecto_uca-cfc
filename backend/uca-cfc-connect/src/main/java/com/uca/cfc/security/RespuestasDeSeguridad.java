package com.uca.cfc.security;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.uca.cfc.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Mantiene el formato de error de la API cuando el rechazo ocurre en los filtros
 * de Spring Security, antes de que se ejecute el ControllerAdvice.
 */
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RespuestasDeSeguridad(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        escribir(request, response, HttpStatus.UNAUTHORIZED,
                "Debe iniciar sesión con un token válido para acceder a este recurso");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN,
                "Su rol no tiene permiso para realizar esta operación");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String message) throws IOException {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(clock), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
