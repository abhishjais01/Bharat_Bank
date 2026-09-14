package com.bank.mock.exception;

import com.bank.mock.cbs.CbsUnavailableException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

// error responses for the bank app, each with the trace id
@RestControllerAdvice
public class BankExceptionHandler {

    // business refusals -> 422
    @ExceptionHandler({InsufficientFundsException.class,
            LimitExceededException.class,
            InvalidBeneficiaryException.class})
    public ResponseEntity<Map<String, Object>> handleRefusal(RuntimeException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // bad statement range -> 400
    @ExceptionHandler(InvalidStatementPeriodException.class)
    public ResponseEntity<Map<String, Object>> handleBadPeriod(RuntimeException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // CBS problem -> 503
    @ExceptionHandler(CbsUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCbsDown(CbsUnavailableException ex) {
        return body(HttpStatus.SERVICE_UNAVAILABLE,
                "The service is temporarily unavailable. Please try again shortly.");
    }

    // common error body
    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {

        return ResponseEntity.status(status).body(Map.of(
                "status", "FAILED",
                "message", message,
                "traceId", String.valueOf(MDC.get("traceId")),
                "timestamp", Instant.now().toString()));
    }
}
