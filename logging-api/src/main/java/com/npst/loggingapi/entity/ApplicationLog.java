package com.npst.loggingapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * One stored application log line.
 *
 * <p>The schema is owned by Flyway ({@code V1__application_logs.sql}); this
 * class only maps onto it, and {@code ddl-auto: validate} makes the two
 * disagreeing a startup failure rather than a silent drift.
 */
@Entity
@Table(name = "application_logs")
public class ApplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "bank_code", nullable = false, length = 16)
    private String bankCode;

    @Column(name = "environment", nullable = false, length = 16)
    private String environment;

    @Column(name = "service", nullable = false, length = 64)
    private String service;

    /**
     * The two context fields support genuinely filters on. Device and IP are
     * diagnostic detail and stay in metadata - this is the high-volume table
     * and a column costs storage on every row.
     */
    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "channel", length = 16)
    private String channel;

    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    @Column(name = "level", nullable = false, length = 8)
    private String level;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Stored as a real MySQL JSON column rather than a serialized string, so
     * metadata stays queryable. Hibernate handles the conversion, which also
     * removes the mapper's manual writeValueAsString and its silent
     * "{}"-on-failure fallback.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "schema_version", nullable = false, length = 8)
    private String schemaVersion;

    /** When the event happened, in the emitting service. */
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    /** When it arrived here. Set by this service, never by the caller. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
