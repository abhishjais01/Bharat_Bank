package com.npst.loggingapi.dto;

import com.npst.loggingapi.entity.ApplicationLog;

import java.time.Instant;
import java.util.Map;

/** One stored application log, as production support sees it. */
public record ApplicationLogResponse(
        Long id,
        String traceId,
        String bankCode,
        String environment,
        String service,
        String customerId,
        String channel,
        String eventType,
        String level,
        String message,
        Map<String, Object> metadata,
        Instant eventTime,
        Instant createdAt) {

    public static ApplicationLogResponse from(ApplicationLog entity) {
        return new ApplicationLogResponse(
                entity.getId(),
                entity.getTraceId(),
                entity.getBankCode(),
                entity.getEnvironment(),
                entity.getService(),
                entity.getCustomerId(),
                entity.getChannel(),
                entity.getEventType(),
                entity.getLevel(),
                entity.getMessage(),
                entity.getMetadata(),
                entity.getEventTime(),
                entity.getCreatedAt());
    }
}
