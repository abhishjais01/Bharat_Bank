package com.npst.loggingapi.mapper;

import com.npst.loggingapi.entity.AuditLog;
import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.masking.MetadataMasker;
import com.npst.observability.util.MaskingUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;

// converts an incoming audit request into a database row
@Component
public class AuditLogMapper {

    private final MetadataMasker masker;

    public AuditLogMapper(MetadataMasker masker) {
        this.masker = masker;
    }

    // copy the fields, masking private details again
    public AuditLog toEntity(AuditIngestRequest request) {

        AuditLog entity = new AuditLog();

        // trace id and service identity
        entity.setTraceId(request.getTraceId());
        entity.setBankCode(request.getBankCode());
        entity.setEnvironment(request.getEnvironment());
        entity.setService(request.getService());

        // caller details
        entity.setChannel(request.getChannel());
        entity.setDeviceId(request.getDeviceId());
        entity.setIpAddress(request.getIpAddress());
        entity.setCustomerId(request.getCustomerId());
        // masked again here because producers other than the starter may send raw values
        entity.setMobileNumber(MaskingUtil.maskMobile(request.getMobileNumber()));

        // who
        entity.setActorId(request.getActorId());
        entity.setActorType(request.getActorType());

        // what
        entity.setAction(request.getAction());
        entity.setModule(request.getModule());
        entity.setEntity(request.getEntity());
        entity.setEntityId(request.getEntityId());
        entity.setDescription(request.getDescription());

        // API call and result
        entity.setApiEndpoint(MaskingUtil.maskIdentifiersInPath(request.getApiEndpoint()));
        entity.setApiMethod(request.getApiMethod());
        entity.setStatusCode(request.getStatusCode());
        entity.setResponseMessage(request.getResponseMessage());
        entity.setDurationMs(request.getDurationMs() == null
                ? null
                : Math.toIntExact(request.getDurationMs()));

        // business details and state change, masked
        entity.setBusinessRef(request.getBusinessRef());
        entity.setAmount(request.getAmount());
        entity.setCurrency(request.getCurrency());
        entity.setBusinessContext(masker.mask(request.getBusinessContext()));

        entity.setBeforeState(masker.mask(request.getBeforeState()));
        entity.setAfterState(masker.mask(request.getAfterState()));

        // event time from the caller, stored time from this service
        entity.setEventTime(request.getTimestamp());
        entity.setCreatedAt(Instant.now());

        return entity;
    }
}
