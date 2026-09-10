package com.npst.loggingapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * One audit record: who did what, to which entity, from where.
 *
 * <p>{@code @Immutable} tells Hibernate never to issue an UPDATE for this
 * entity. It pairs with the database trigger from V2, which rejects UPDATE and
 * DELETE outright - an audit trail protected only by application convention is
 * not protected.
 *
 * <p>A third layer belongs here and is not yet applied: the application user
 * should hold INSERT and SELECT on this table and nothing more. Today it holds
 * ALL PRIVILEGES, so the trigger is the only thing standing between a stray
 * DELETE and the audit history. See ARCHITECTURE.md, section 8.
 *
 * <p>Identity fields here are <b>unmasked</b> by policy. A regulator asking who
 * moved money cannot work with a masked value. The copy written to the log file
 * is masked instead.
 */
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

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

    // --- where the request came from -----------------------------------------

    @Column(name = "channel", length = 16)
    private String channel;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    // --- who -----------------------------------------------------------------

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;

    // --- what ----------------------------------------------------------------

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "module", length = 64)
    private String module;

    @Column(name = "entity", nullable = false, length = 64)
    private String entity;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // --- the API call --------------------------------------------------------

    @Column(name = "api_endpoint", length = 255)
    private String apiEndpoint;

    @Column(name = "api_method", length = 8)
    private String apiMethod;

    /**
     * SMALLINT in the schema - an HTTP status is three digits and never needs
     * four bytes. Integer in Java because that is what the contract carries and
     * what callers expect; the columnDefinition reconciles the two for
     * ddl-auto: validate.
     */
    @Column(name = "status_code", columnDefinition = "smallint")
    private Integer statusCode;

    @Column(name = "response_message", length = 512)
    private String responseMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    // --- business fields -----------------------------------------------------

    @Column(name = "business_ref", length = 64)
    private String businessRef;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * CHAR, not VARCHAR. An ISO 4217 code is always exactly three characters,
     * so the fixed width is both correct and marginally cheaper - but Hibernate
     * maps String to VARCHAR unless told otherwise, and ddl-auto: validate
     * refuses to start on the mismatch.
     */
    @Column(name = "currency", length = 3, columnDefinition = "char(3)")
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_context")
    private Map<String, Object> businessContext;

    // --- state transition ----------------------------------------------------

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private Map<String, Object> beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private Map<String, Object> afterState;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // --- tamper evidence -----------------------------------------------------

    /**
     * CHAR(64) like the migration: a SHA-256 hex digest is always exactly 64
     * characters. Null only on the first row of the chain.
     */
    @Column(name = "prev_hash", length = 64, columnDefinition = "char(64)")
    private String prevHash;

    @Column(name = "row_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String rowHash;

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

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public String getBusinessRef() {
        return businessRef;
    }

    public void setBusinessRef(String businessRef) {
        this.businessRef = businessRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getBusinessContext() {
        return businessContext;
    }

    public void setBusinessContext(Map<String, Object> businessContext) {
        this.businessContext = businessContext;
    }

    public Map<String, Object> getBeforeState() {
        return beforeState;
    }

    public void setBeforeState(Map<String, Object> beforeState) {
        this.beforeState = beforeState;
    }

    public Map<String, Object> getAfterState() {
        return afterState;
    }

    public void setAfterState(Map<String, Object> afterState) {
        this.afterState = afterState;
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

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getRowHash() {
        return rowHash;
    }

    public void setRowHash(String rowHash) {
        this.rowHash = rowHash;
    }
}
