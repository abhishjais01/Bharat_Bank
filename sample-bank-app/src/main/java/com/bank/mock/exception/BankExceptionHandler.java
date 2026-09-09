package com.bank.mock.exception;

import com.bank.mock.cbs.CbsUnavailableException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * The bank application's own error responses.
 *
 * <p>Note that this does no logging. The aspect has already recorded the
 * failure, at WARN or ERROR as appropriate, before the exception reaches here -
 * so this class is only concerned with what the customer sees.
 *
 * <p>Every response carries the trace id, which is what turns a customer saying
 * "my transfer failed" into a single search.
 */
@RestControllerAdvice
public class BankExceptionHandler {

    /** Refusals. The request was understood and correctly declined. */
    @ExceptionHandler({InsufficientFundsException.class,
            LimitExceededException.class,
            InvalidBeneficiaryException.class})
    public ResponseEntity<Map<String, Object>> handleRefusal(RuntimeException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatementPeriodException.class)
    public ResponseEntity<Map<String, Object>> handleBadPeriod(RuntimeException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** A genuine fault. The customer may retry. */
    @ExceptionHandler(CbsUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCbsDown(CbsUnavailableException ex) {
        return body(HttpStatus.SERVICE_UNAVAILABLE,
                "The service is temporarily unavailable. Please try again shortly.");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {

        return ResponseEntity.status(status).body(Map.of(
                "status", "FAILED",
                "message", message,
                "traceId", String.valueOf(MDC.get("traceId")),
                "timestamp", Instant.now().toString()));
    }
}
