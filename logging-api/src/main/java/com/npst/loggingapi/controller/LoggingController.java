package com.npst.loggingapi.controller;

import com.npst.loggingapi.service.LoggingService;
import com.npst.observability.contract.ApiResponse;
import com.npst.observability.contract.LogIngestRequest;
import com.npst.observability.contract.LogIngestResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class LoggingController {

    private final LoggingService service;

    public LoggingController(LoggingService service) {
        this.service = service;
    }

    /**
     * Ingests one application log.
     *
     * <p>Returns 201 rather than the previous 202. The row is written inside
     * this call, so "accepted for processing later" was a false promise - and a
     * caller retrying on the strength of it would have duplicated the row.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LogIngestResponse>> ingest(
            @Valid @RequestBody LogIngestRequest request) {

        Long logId = service.store(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Log stored", traceId(request),
                        new LogIngestResponse(logId)));
    }

    /**
     * Prefers the trace id of the log being reported over this request's own,
     * so a rejection can be correlated with the customer journey that produced
     * it rather than with the logging call.
     */
    private static String traceId(LogIngestRequest request) {

        if (request != null && request.getTraceId() != null && !request.getTraceId().isBlank()) {
            return request.getTraceId();
        }

        return MDC.get("traceId");
    }
}
