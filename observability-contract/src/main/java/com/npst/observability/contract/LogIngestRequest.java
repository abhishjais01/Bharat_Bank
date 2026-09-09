package com.npst.observability.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * The payload POSTed to {@code /api/v1/logs}.
 *
 * <p>This is the single definition of the wire format. Before it existed the
 * SDK serialized its internal event object and logging-api validated a
 * separate DTO; the two matched only by coincidence, and at one point did not
 * match at all ({@code serviceName} vs {@code service}).
 *
 * <p>Every field except {@code metadata} is populated automatically by the
 * SDK. A developer in a banking service supplies only a message and metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogIngestRequest {

    /** Current wire format version. Bump when a field changes meaning. */
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    /**
     * Lets logging-api keep accepting older producers during a rolling
     * deployment, rather than rejecting services that have not restarted yet.
     */
    @NotBlank(message = "schemaVersion is required")
    private String schemaVersion = CURRENT_SCHEMA_VERSION;

    /** Correlation id, generated at the gateway and propagated downstream. */
    @NotBlank(message = "traceId is required")
    private String traceId;

    @NotBlank(message = "bankCode is required")
    private String bankCode;

    @NotBlank(message = "environment is required")
    private String environment;

    /** Logical name of the emitting microservice, e.g. account-service. */
    @NotBlank(message = "service is required")
    private String service;

    /**
     * Request context, captured at the service edge. Optional - a scheduled job
     * or an internal service-to-service call has no channel or customer.
     */
    private String channel;

    private String deviceId;

    private String ipAddress;

    private String customerId;

    @NotNull(message = "eventType is required")
    private EventType eventType;

    @NotNull(message = "level is required")
    private LogLevel level;

    @NotBlank(message = "message is required")
    private String message;

    /**
     * When the event happened in the emitting service - distinct from the row's
     * ingest time, which logging-api stamps on arrival. Clocks across services
     * are never perfectly aligned, so both are kept.
     */
    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    /** Free-form business context. Masked by the SDK before it is sent. */
    private Map<String, Object> metadata;

    public LogIngestRequest() {
    }

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
