package com.npst.observability.mapper;

import com.npst.observability.contract.LogIngestRequest;
import com.npst.observability.schema.LogEvent;

/**
 * Converts the SDK's internal event model into the wire contract.
 *
 * <p>Keeping these apart is deliberate. The file/Loki log carries the richer
 * internal shape (audit actor, stack trace, endpoint); the HTTP payload
 * carries only what logging-api has agreed to accept. Previously the internal
 * object was serialized straight onto the wire, which is how the
 * {@code serviceName} / {@code service} mismatch went unnoticed.
 */
public final class LogEventMapper {

    private LogEventMapper() {
    }

    public static LogIngestRequest toIngestRequest(LogEvent event) {

        LogIngestRequest request = new LogIngestRequest();

        request.setSchemaVersion(LogIngestRequest.CURRENT_SCHEMA_VERSION);
        request.setTraceId(event.getTraceId());
        request.setBankCode(event.getBankCode());
        request.setEnvironment(event.getEnvironment());
        request.setService(event.getService());
        request.setEventType(event.getEventType());
        request.setLevel(event.getLevel());
        request.setMessage(event.getMessage());
        request.setTimestamp(event.getTimestamp());
        request.setMetadata(event.getMetadata());

        return request;
    }
}
