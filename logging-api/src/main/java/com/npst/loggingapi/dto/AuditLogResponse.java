package com.npst.loggingapi.dto;

import com.npst.loggingapi.entity.AuditLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * One audit record, as compliance sees it.
 *
 * <p>Includes prevHash and rowHash on purpose: an auditor should be able to
 * walk the chain from the API alone, without database access.
 */
public record AuditLogResponse(
        Long id,
        String traceId,
        String bankCode,
        String environment,
        String service,
        String channel,
        String deviceId,
        String ipAddress,
        String customerId,
        String mobileNumber,
        String actorId,
        String actorType,
        String action,
        String module,
        String entity,
        String entityId,
        String description,
        String apiEndpoint,
        String apiMethod,
        Integer statusCode,
        String responseMessage,
        Integer durationMs,
        String businessRef,
        BigDecimal amount,
        String currency,
        Map<String, Object> businessContext,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        Instant eventTime,
        Instant createdAt,
        String prevHash,
        String rowHash) {

    public static AuditLogResponse from(AuditLog entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getTraceId(),
                entity.getBankCode(),
                entity.getEnvironment(),
                entity.getService(),
                entity.getChannel(),
                entity.getDeviceId(),
                entity.getIpAddress(),
                entity.getCustomerId(),
                entity.getMobileNumber(),
                entity.getActorId(),
                entity.getActorType(),
                entity.getAction(),
                entity.getModule(),
                entity.getEntity(),
                entity.getEntityId(),
                entity.getDescription(),
                entity.getApiEndpoint(),
                entity.getApiMethod(),
                entity.getStatusCode(),
                entity.getResponseMessage(),
                entity.getDurationMs(),
                entity.getBusinessRef(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getBusinessContext(),
                entity.getBeforeState(),
                entity.getAfterState(),
                entity.getEventTime(),
                entity.getCreatedAt(),
                entity.getPrevHash(),
                entity.getRowHash());
    }
}
