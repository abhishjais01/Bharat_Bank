package com.npst.observability.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Optional catch-all that turns an unhandled exception into a trace-carrying
 * error body.
 *
 * <p>Opt-in via {@code observability.exception-handler.enabled=true}. It is off
 * by default on purpose: a starter that silently installs a handler for
 * {@code Exception} in every importing service would swallow the application's
 * own error handling. Ordered last so any application advice wins.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {

        String traceId = MDC.get("traceId");

        log.error("Unhandled Exception : traceId={} message={}", traceId, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                traceId,
                "ERR-500",
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
