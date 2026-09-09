package com.npst.loggingapi.controller;

import com.npst.loggingapi.dto.AuditLogResponse;
import com.npst.loggingapi.dto.PageResponse;
import com.npst.loggingapi.search.AuditSearchCriteria;
import com.npst.loggingapi.service.AuditQueryService;
import com.npst.loggingapi.service.AuditService;
import com.npst.observability.contract.ApiResponse;
import com.npst.observability.contract.AuditIngestRequest;
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

/**
 * The audit trail: who did what, to which record, from where.
 *
 * <p>A separate endpoint from application logs rather than a flag on the same
 * one. The two have different shapes, different readers, different retention -
 * years against days - and different access control, since audit records carry
 * unmasked identity. Sharing a route would have meant sharing all of that.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit trail", description = "Append and search the immutable audit trail")
public class AuditController {

    private final AuditService auditService;
    private final AuditQueryService queryService;

    public AuditController(AuditService auditService, AuditQueryService queryService) {
        this.auditService = auditService;
        this.queryService = queryService;
    }

    /**
     * Appends one audit record. There is deliberately no update and no delete -
     * the table rejects both at the database level.
     */
    @PostMapping
    @Operation(summary = "Append one audit record to the chain")
    public ResponseEntity<ApiResponse<LogIngestResponse>> append(
            @Valid @RequestBody AuditIngestRequest request) {

        Long auditId = auditService.append(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audit record stored", request.getTraceId(),
                        new LogIngestResponse(auditId)));
    }

    @GetMapping("/{traceId}")
    @Operation(summary = "Every audit record written under one trace id")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> byTraceId(
            @PathVariable String traceId) {

        List<AuditLogResponse> records = queryService.findByTraceId(traceId).stream()
                .map(AuditLogResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                records.isEmpty() ? "No audit records for this trace id" : "Records retrieved",
                traceId,
                records));
    }

    /**
     * Filtered search, shaped around the questions compliance asks: what did
     * this customer do, who performed this action, what happened to this
     * entity, which transfers carry this reference.
     */
    @GetMapping
    @Operation(summary = "Search the audit trail by actor, customer, action, module or entity")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> search(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String businessRef,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        AuditSearchCriteria criteria = new AuditSearchCriteria(
                actorId, customerId, action, module, entity, entityId,
                channel, businessRef, from, to);

        Page<AuditLogResponse> page =
                queryService.search(criteria, pageable).map(AuditLogResponse::from);

        return ResponseEntity.ok(ApiResponse.success("Search complete", MDC.get("traceId"),
                PageResponse.of(page, response -> response)));
    }
}
