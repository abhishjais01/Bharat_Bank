package com.npst.observability.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * The payload POSTed to {@code /api/v1/audit}.
 *
 * <p>Separate from {@link LogIngestRequest} on purpose. An audit record answers
 * "who did what", is read by compliance rather than support, is retained for
 * years rather than days, and - unlike an application log - keeps identity
 * fields unmasked. Sharing one shape with application logs would have meant one
 * set of rules for two very different obligations.
 *
 * <p><b>This carries unmasked PII.</b> customerId and mobileNumber are the real
 * values, because a regulator asking who moved money cannot work with
 * {@code XXXXXXXX5510}. The same event written to the log file is masked; only
 * what travels to the audit table keeps identity intact.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditIngestRequest {

    public static final String CURRENT_SCHEMA_VERSION = "1.0";

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

    // --- who -----------------------------------------------------------------

    @NotBlank(message = "actorId is required")
    private String actorId;

    @NotBlank(message = "actorType is required")
    private String actorType;

    // --- what ----------------------------------------------------------------

    @NotBlank(message = "action is required")
    private String action;

    /** Banking domain, from {@code @LogRegistry(module = ...)}. */
    private String module;

    @NotBlank(message = "entity is required")
    private String entity;

    private String entityId;

    private String description;

    // --- where the request came from -----------------------------------------

    private String channel;

    private String deviceId;

    private String ipAddress;

    /** Unmasked. See the class comment. */
    private String customerId;

    /** Unmasked. See the class comment. */
    private String mobileNumber;

    // --- the API call --------------------------------------------------------

    private String apiEndpoint;

    private String apiMethod;

    private Integer statusCode;

    private String responseMessage;

    private Long durationMs;

    // --- business fields -----------------------------------------------------

    /** Transaction or request reference the business would quote. */
    private String businessRef;

    private BigDecimal amount;

    /** ISO 4217, e.g. INR. */
    private String currency;

    /** Domain extras with no column of their own. */
    private Map<String, Object> businessContext;

    // --- state transition, per the PRD's Auditability NFR ---------------------

    private Map<String, Object> beforeState;

    private Map<String, Object> afterState;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    public AuditIngestRequest() {
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
