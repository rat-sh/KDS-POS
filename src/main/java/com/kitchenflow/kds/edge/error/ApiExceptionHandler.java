package com.kitchenflow.kds.edge.error;

import com.kitchenflow.kds.common.error.ApiFailure;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.sql.SQLException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApiFailure.class)
    ResponseEntity<?> apiFailure(ApiFailure failure, HttpServletRequest request) {
        return response(failure.status(), failure.code(), failure.getMessage(), request, failure.details(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException failure, HttpServletRequest request) {
        List<FieldIssue> issues = failure.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldIssue(error.getField(), fieldCode(error.getCode()),
                        error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()))
                .toList();
        return response(422, "VALIDATION_FAILED", "The request contains invalid fields.", request, Map.of(), issues);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> badRequest(Exception failure, HttpServletRequest request) {
        return response(400, "INVALID_FORMAT", "The request body or one of its values is invalid.", request, Map.of(), List.of());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<?> notFound(HttpServletRequest request) {
        return response(404, "NOT_FOUND", "The requested record was not found.", request, Map.of(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(HttpServletRequest request) {
        return response(409, "INVALID_TRANSITION", "The value conflicts with an existing record or database rule.", request, Map.of(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> forbidden(HttpServletRequest request) {
        return response(403, "FORBIDDEN", "You do not have permission to perform this action.", request, Map.of(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception failure, HttpServletRequest request) {
        if (isRetryableDatabaseFailure(failure)) {
            return response(409, "RETRYABLE", "The request conflicted with another database operation. Retry it.", request, Map.of(), List.of());
        }
        return response(500, "INTERNAL_ERROR", "The request could not be completed.", request, Map.of(), List.of());
    }

    private boolean isRetryableDatabaseFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && ("40P01".equals(sqlException.getSQLState()) || "40001".equals(sqlException.getSQLState()))) return true;
        }
        return false;
    }

    private ResponseEntity<ErrorBody> response(int status, String code, String message, HttpServletRequest request,
            Map<String, Object> details, List<FieldIssue> errors) {
        Object correlation = request.getAttribute("correlationId");
        return ResponseEntity.status(HttpStatusCode.valueOf(status)).body(new ErrorBody(clock.instant(), status, code,
                message, request.getRequestURI(), correlation == null ? "" : correlation.toString(), details, errors));
    }

    private static String fieldCode(String code) {
        if (code == null) return "INVALID_FORMAT";
        return switch (code) {
            case "NotBlank", "NotNull", "NotEmpty" -> "REQUIRED";
            case "Size" -> "TOO_LONG";
            case "Min", "Max", "DecimalMin", "DecimalMax" -> "OUT_OF_RANGE";
            case "Pattern" -> "INVALID_FORMAT";
            default -> "INVALID_FORMAT";
        };
    }

    public record ErrorBody(Instant timestamp, int status, String code, String message, String path,
            String correlationId, Map<String, Object> details, List<FieldIssue> errors) {}
    public record FieldIssue(String field, String code, String message) {}

}
