package com.npst.observability.mapper;

import com.npst.observability.contract.LogIngestRequest;
import com.npst.observability.schema.LogEvent;

// converts the internal log event into the JSON sent to logging-api
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
        request.setChannel(event.getChannel());
        request.setDeviceId(event.getDeviceId());
        request.setIpAddress(event.getIpAddress());
        request.setCustomerId(event.getCustomerId());
        request.setEventType(event.getEventType());
        request.setLevel(event.getLevel());
        request.setMessage(event.getMessage());
        request.setTimestamp(event.getTimestamp());
        request.setMetadata(event.getMetadata());

        return request;
    }
}
