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

/**
 * Turns every failure mode of the ingest endpoint into the same envelope.
 *
 * <p>Without this, validation failures fell through to Spring's default
 * ProblemDetail body: no traceId, no field list, nothing a support engineer
 * could correlate. The starter's own GlobalExceptionHandler could not fill the
 * gap either - it lives in a package this service never scans, and it maps
 * everything to 500, which would have turned a caller's bad request into an
 * apparent server fault.
 *
 * <p>Status codes are chosen to tell a producer what to do:
 * <ul>
 *   <li>400 - the request is malformed. Retrying will not help.</li>
 *   <li>422 - well formed, but this layer does not persist that event type.
 *       A configuration problem in the producer, not a transport error.</li>
 *   <li>500 - ours. The producer may retry.</li>
 * </ul>
 */
@RestControllerAdvice
public class LoggingApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingApiExceptionHandler.class);

    /** Missing or blank required fields - the five the SDK must enrich. */
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

    /** Unparseable JSON, or a value outside an enum such as level=CRITICAL. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {

        log.warn("Rejected log : traceId={} unreadable body : {}", traceId(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Log rejected: request body could not be read",
                        traceId(), List.of(rootCauseOf(ex))));
    }

    @ExceptionHandler(UnsupportedEventTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedEventType(
            UnsupportedEventTypeException ex) {

        log.warn("Rejected log : traceId={} eventType={} not persisted in Layer 1",
                traceId(), ex.getEventType());

        return ResponseEntity.unprocessableEntity()
                .body(ApiResponse.error("Log rejected: unsupported event type",
                        traceId(), List.of(ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {

        log.error("Log ingest failed : traceId={}", traceId(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Log ingest failed", traceId(), null));
    }

    private static String traceId() {
        return MDC.get("traceId");
    }

    /**
     * Jackson's message names the offending field and value, which is exactly
     * what the producer needs. The wrapper's message is noise.
     */
    private static String rootCauseOf(Exception ex) {

        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        String message = cause.getMessage();

        if (message == null) {
            return cause.getClass().getSimpleName();
        }

        // Jackson appends the full class path and byte offset; the first line
        // carries the actual problem.
        return message.lines().findFirst().orElse(message);
    }
}
