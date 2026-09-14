package com.npst.loggingapi.exception;

import com.npst.observability.contract.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

// turns errors into the standard ApiResponse with a clear status code
@RestControllerAdvice
public class LoggingApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingApiExceptionHandler.class);

    // 400: required fields missing
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.toList());

        log.warn("Rejected log : traceId={} errors={}", traceId(), errors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Log rejected: validation failed", traceId(), errors));
    }

    // 400: body is not valid JSON or has a bad value
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {

        log.warn("Rejected log : traceId={} unreadable body : {}", traceId(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Log rejected: request body could not be read",
                        traceId(), List.of(rootCauseOf(ex))));
    }

    // 422: wrong event type for this endpoint
    @ExceptionHandler(UnsupportedEventTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedEventType(
            UnsupportedEventTypeException ex) {

        log.warn("Rejected log : traceId={} eventType={} not persisted in Layer 1",
                traceId(), ex.getEventType());

        return ResponseEntity.unprocessableEntity()
                .body(ApiResponse.error("Log rejected: unsupported event type",
                        traceId(), List.of(ex.getMessage())));
    }

    // 500: anything unexpected, e.g. database down
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {

        log.error("Log ingest failed : traceId={}", traceId(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Log ingest failed", traceId(), null));
    }

    // trace id of this request
    private static String traceId() {
        return MDC.get("traceId");
    }

    // first line of the real parsing error
    private static String rootCauseOf(Exception ex) {

        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        String message = cause.getMessage();

        if (message == null) {
            return cause.getClass().getSimpleName();
        }

        return message.lines().findFirst().orElse(message);
    }
}
