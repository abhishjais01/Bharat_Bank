package com.npst.loggingapi.controller;

import com.npst.loggingapi.dto.ApplicationLogResponse;
import com.npst.loggingapi.dto.PageResponse;
import com.npst.loggingapi.search.LogSearchCriteria;
import com.npst.loggingapi.service.LoggingService;
import com.npst.observability.contract.ApiResponse;
import com.npst.observability.contract.LogIngestRequest;
import com.npst.observability.contract.LogIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "Application logs", description = "Ingest and search application logs")
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
    @Operation(summary = "Store one application log")
    public ResponseEntity<ApiResponse<LogIngestResponse>> ingest(
            @Valid @RequestBody LogIngestRequest request) {

        Long logId = service.store(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Log stored", traceId(request),
                        new LogIngestResponse(logId)));
    }

    /**
     * The support path: paste a trace id, get the whole customer journey.
     *
     * <p>Returns a list, not a single record. One trace spans several services
     * and many lines - collapsing that to one row would defeat the purpose of
     * having a correlation id at all.
     */
    @GetMapping("/{traceId}")
    @Operation(summary = "Every log line recorded under one trace id")
    public ResponseEntity<ApiResponse<List<ApplicationLogResponse>>> byTraceId(
            @PathVariable String traceId) {

        List<ApplicationLogResponse> journey = service.findByTraceId(traceId).stream()
                .map(ApplicationLogResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                journey.isEmpty() ? "No logs found for this trace id" : "Journey retrieved",
                traceId,
                journey));
    }

    /**
     * Filtered search. Every parameter is optional and they combine freely;
     * supplying none returns the most recent logs.
     */
    @GetMapping
    @Operation(summary = "Search application logs by service, level, customer or channel")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationLogResponse>>> search(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        LogSearchCriteria criteria = new LogSearchCriteria(
                service, level, environment, customerId, channel, from, to);

        Page<ApplicationLogResponse> page =
                this.service.search(criteria, pageable).map(ApplicationLogResponse::from);

        return ResponseEntity.ok(ApiResponse.success("Search complete", MDC.get("traceId"),
                PageResponse.of(page, response -> response)));
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
