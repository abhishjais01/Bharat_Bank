package com.npst.observability.schema;

import com.npst.observability.contract.EventType;

import java.math.BigDecimal;
import java.util.Map;

// internal audit record: who did what, to which record, and the result
public class AuditEvent extends LogEvent {

    // who
    private String actorId;
    private String actorType;

    // what
    private String action;
    private String module;
    private String entity;
    private String entityId;
    private String description;

    // customer mobile (masked before sending)
    private String mobileNumber;

    // API call and result
    private String apiEndpoint;
    private String apiMethod;
    private Integer statusCode;
    private String responseMessage;
    private Long durationMs;

    // business details
    private String businessRef;
    private BigDecimal amount;
    private String currency;
    private Map<String, Object> businessContext;

    // state before and after the change
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;

    // every audit event has type AUDIT
    public AuditEvent() {
        setEventType(EventType.AUDIT);
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
}
