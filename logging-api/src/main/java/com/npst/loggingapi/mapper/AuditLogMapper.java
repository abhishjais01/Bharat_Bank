package com.npst.loggingapi.mapper;

import com.npst.loggingapi.entity.AuditLog;
import com.npst.observability.contract.AuditIngestRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditLogMapper {

    public AuditLog toEntity(AuditIngestRequest request) {

        AuditLog entity = new AuditLog();

        entity.setTraceId(request.getTraceId());
        entity.setBankCode(request.getBankCode());
        entity.setEnvironment(request.getEnvironment());
        entity.setService(request.getService());

        entity.setChannel(request.getChannel());
        entity.setDeviceId(request.getDeviceId());
        entity.setIpAddress(request.getIpAddress());
        entity.setCustomerId(request.getCustomerId());
        entity.setMobileNumber(request.getMobileNumber());

        entity.setActorId(request.getActorId());
        entity.setActorType(request.getActorType());

        entity.setAction(request.getAction());
        entity.setModule(request.getModule());
        entity.setEntity(request.getEntity());
        entity.setEntityId(request.getEntityId());
        entity.setDescription(request.getDescription());

        entity.setApiEndpoint(request.getApiEndpoint());
        entity.setApiMethod(request.getApiMethod());
        entity.setStatusCode(request.getStatusCode());
        entity.setResponseMessage(request.getResponseMessage());
        entity.setDurationMs(request.getDurationMs() == null
                ? null
                : Math.toIntExact(request.getDurationMs()));

        entity.setBusinessRef(request.getBusinessRef());
        entity.setAmount(request.getAmount());
        entity.setCurrency(request.getCurrency());
        entity.setBusinessContext(request.getBusinessContext());

        entity.setBeforeState(request.getBeforeState());
        entity.setAfterState(request.getAfterState());

        entity.setEventTime(request.getTimestamp());
        entity.setCreatedAt(Instant.now());

        return entity;
    }
}
