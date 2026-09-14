package com.npst.observability.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

// JSON body sent to POST /api/v1/logs, one application log
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogIngestRequest {

    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    // required fields, filled automatically by the starter
    @NotBlank(message = "schemaVersion is required")
    private String schemaVersion = CURRENT_SCHEMA_VERSION;

    @NotBlank(message = "traceId is required")
    private String traceId;

    @NotBlank(message = "bankCode is required")
    private String bankCode;

    @NotBlank(message = "environment is required")
    private String environment;

    @NotBlank(message = "service is required")
    private String service;

    // optional details about the caller
    private String channel;

    private String deviceId;

    private String ipAddress;

    private String customerId;

    // what kind of log and how serious
    @NotNull(message = "eventType is required")
    private EventType eventType;

    @NotNull(message = "level is required")
    private LogLevel level;

    @NotBlank(message = "message is required")
    private String message;

    // when it happened, plus any extra details
    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    private Map<String, Object> metadata;

    public LogIngestRequest() {
    }

    // getters and setters
    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
