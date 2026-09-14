package com.npst.observability.mapper;

import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.schema.AuditEvent;

// converts the internal audit event into the JSON sent to logging-api
public final class AuditEventMapper {

    private AuditEventMapper() {
    }

    public static AuditIngestRequest toIngestRequest(AuditEvent event) {

        AuditIngestRequest request = new AuditIngestRequest();

        request.setSchemaVersion(AuditIngestRequest.CURRENT_SCHEMA_VERSION);
        request.setTraceId(event.getTraceId());
        request.setBankCode(event.getBankCode());
        request.setEnvironment(event.getEnvironment());
        request.setService(event.getService());

        // who
        request.setActorId(event.getActorId());
        request.setActorType(event.getActorType());

        // what
        request.setAction(event.getAction());
        request.setModule(event.getModule());
        request.setEntity(event.getEntity());
        request.setEntityId(event.getEntityId());
        request.setDescription(event.getDescription());

        // where from
        request.setChannel(event.getChannel());
        request.setDeviceId(event.getDeviceId());
        request.setIpAddress(event.getIpAddress());
        request.setCustomerId(event.getCustomerId());
        request.setMobileNumber(event.getMobileNumber());

        // API call and result
        request.setApiEndpoint(event.getApiEndpoint());
        request.setApiMethod(event.getApiMethod());
        request.setStatusCode(event.getStatusCode());
        request.setResponseMessage(event.getResponseMessage());
        request.setDurationMs(event.getDurationMs());

        // business details
        request.setBusinessRef(event.getBusinessRef());
        request.setAmount(event.getAmount());
        request.setCurrency(event.getCurrency());
        request.setBusinessContext(event.getBusinessContext());

        // state change
        request.setBeforeState(event.getBeforeState());
        request.setAfterState(event.getAfterState());

        request.setTimestamp(event.getTimestamp());

        return request;
    }
}
