package com.uca.cfc.exception;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Manejo centralizado de errores. Todas las respuestas usan {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Pattern ATRIBUTO_NO_RESUELTO =
            Pattern.compile("Could not resolve attribute '([^']+)'");

    private static final String SQL_FOREIGN_KEY = "23503";
    private static final String SQL_UNIQUE = "23505";
    private static final String SQL_CHECK = "23514";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.debug("Regla de negocio: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.add(new ErrorResponse.CampoError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.add(new ErrorResponse.CampoError(error.getObjectName(),
                        error.getDefaultMessage())));
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", request, errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorResponse.CampoError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene parámetros inválidos", request, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ErrorResponse.CampoError> errores = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.CampoError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        String mensaje = "El cuerpo de la solicitud no es un JSON válido o tiene un formato incorrecto";
        if (NestedExceptionUtils.getMostSpecificCause(ex) instanceof InvalidFormatException invalid) {
            String campo = invalid.getPath().isEmpty() ? "desconocido"
                    : invalid.getPath().getLast().getPropertyName();
            mensaje = "Valor inválido '" + invalid.getValue() + "' para el campo '" + campo + "'";
            Class<?> tipo = invalid.getTargetType();
            if (tipo != null && tipo.isEnum()) {
                mensaje += ". Valores permitidos: " + List.of(tipo.getEnumConstants());
            }
        }
        return build(HttpStatus.BAD_REQUEST, mensaje, request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String mensaje = "Valor inválido '" + ex.getValue() + "' para el parámetro '" + ex.getName() + "'";
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            mensaje += ". Valores permitidos: " + List.of(tipo.getEnumConstants());
        }
        return build(HttpStatus.BAD_REQUEST, mensaje, request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "El parámetro obligatorio '" + ex.getParameterName() + "' no fue enviado", request, List.of());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "No se puede ordenar por '" + ex.getPropertyName() + "': el campo no existe", request, List.of());
    }

    /**
     * Cuando el repositorio usa una consulta @Query propia, Spring Data no valida
     * el parámetro ?sort= de antemano: el nombre del campo se concatena a la
     * consulta y el error llega desde Hibernate. Puede venir de dos formas:
     *
     * <ul>
     *   <li>el campo no existe en la entidad ("Could not resolve attribute");</li>
     *   <li>el texto ni siquiera es un nombre de campo válido y rompe la sintaxis
     *       de la consulta (por ejemplo {@code ?sort=["string"]}).</li>
     * </ul>
     *
     * Ambas son culpa de lo que envió el cliente, así que la respuesta es 400 y
     * no 500. Esta excepción siempre indica un uso incorrecto de la API.
     */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex,
            HttpServletRequest request) {
        if (ex.getCause() instanceof PropertyReferenceException propertyEx) {
            return handlePropertyReference(propertyEx, request);
        }

        // El error de Hibernate puede venir anidado a varios niveles
        String causaRaiz = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        Matcher atributo = ATRIBUTO_NO_RESUELTO.matcher(causaRaiz == null ? "" : causaRaiz);
        if (atributo.find()) {
            return build(HttpStatus.BAD_REQUEST,
                    "No se puede ordenar o filtrar por '" + atributo.group(1) + "': el campo no existe",
                    request, List.of());
        }

        String orden = request.getParameter("sort");
        if (orden != null && !orden.isBlank()) {
            log.debug("Parámetro de ordenamiento inválido '{}' en {}", orden, request.getRequestURI());
            return build(HttpStatus.BAD_REQUEST, "El parámetro de ordenamiento '" + orden
                    + "' no es válido. Use el formato ?sort=campo,asc con un campo de la entidad",
                    request, List.of());
        }

        log.warn("Consulta mal construida en {}: {}", request.getRequestURI(), causaRaiz);
        return build(HttpStatus.BAD_REQUEST,
                "La consulta no pudo construirse con los parámetros enviados", request, List.of());
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String sqlState = null;
        if (NestedExceptionUtils.getMostSpecificCause(ex) instanceof SQLException sqlEx) {
            sqlState = sqlEx.getSQLState();
        }
        String mensaje;
        if (SQL_FOREIGN_KEY.equals(sqlState)) {
            mensaje = "La operación no es posible: el registro está relacionado con otros datos del sistema";
        } else if (SQL_UNIQUE.equals(sqlState)) {
            mensaje = "Ya existe un registro con uno de los valores que deben ser únicos";
        } else if (SQL_CHECK.equals(sqlState)) {
            mensaje = "Los datos no cumplen una restricción de la base de datos";
        } else {
            mensaje = "La operación viola la integridad de los datos";
        }
        log.warn("Violación de integridad [{}]: {}", sqlState, NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
        return build(HttpStatus.CONFLICT, mensaje, request, List.of());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(ConcurrencyFailureException ex,
            HttpServletRequest request) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT,
                "Otro usuario modificó el mismo recurso al mismo tiempo. Intente nuevamente", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está permitido en esta ruta", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de contenido no soportado. Use application/json", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Si persiste, contacte al administrador", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
            List<ErrorResponse.CampoError> errores) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(clock), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
