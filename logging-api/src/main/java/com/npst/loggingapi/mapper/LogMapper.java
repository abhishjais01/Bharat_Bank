package com.npst.loggingapi.mapper;

import com.npst.loggingapi.entity.ApplicationLog;
import com.npst.observability.contract.LogIngestRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Wire contract to stored row.
 *
 * <p>Note the two timestamps. {@code event_time} is the producer's, carried on
 * the request; {@code created_at} is stamped here on arrival. Keeping both is
 * what makes a trace readable when one service's clock has drifted.
 */
@Component
public class LogMapper {

    public ApplicationLog toEntity(LogIngestRequest request) {

        ApplicationLog entity = new ApplicationLog();

        entity.setTraceId(request.getTraceId());
        entity.setBankCode(request.getBankCode());
        entity.setEnvironment(request.getEnvironment());
        entity.setService(request.getService());
        entity.setCustomerId(request.getCustomerId());
        entity.setChannel(request.getChannel());
        entity.setEventType(request.getEventType().name());
        entity.setLevel(request.getLevel().name());
        entity.setMessage(request.getMessage());
        entity.setMetadata(request.getMetadata());
        entity.setSchemaVersion(request.getSchemaVersion());
        entity.setEventTime(request.getTimestamp());
        entity.setCreatedAt(Instant.now());

        return entity;
    }
}
