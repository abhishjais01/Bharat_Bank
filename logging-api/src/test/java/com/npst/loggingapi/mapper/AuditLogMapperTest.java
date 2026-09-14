package com.npst.loggingapi.mapper;

import com.npst.loggingapi.entity.AuditLog;
import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.masking.MetadataMasker;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// checks logging-api masks private details before storing
class AuditLogMapperTest {

    private final AuditLogMapper mapper =
            new AuditLogMapper(new MetadataMasker(new ObservabilityProperties.Masking()));

    @Test
    void masksPrivateCustomerDetailsEvenWhenTheProducerSentThemRaw() {

        AuditLog entity = mapper.toEntity(rawRequest());

        assertThat(entity.getMobileNumber()).isEqualTo("98XXXX3210");
        assertThat(entity.getApiEndpoint()).isEqualTo("/api/v1/accounts/XXXXXXXX5510/limit");
        assertThat(entity.getBusinessContext()).containsEntry("beneficiaryAccount", "XXXXXXXX5599");
        assertThat(entity.getBeforeState()).containsEntry("email", "rXXXXX@bharatbank.in");
        assertThat(entity.getAfterState())
                .containsEntry("accountNumber", "XXXXXXXX5510")
                .containsEntry("beneficiaryName", "RXXXXX AXXX")
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void keepsTheValuesTheAuditTrailNeedsToIdentifyWhoDidWhat() {

        AuditLog entity = mapper.toEntity(rawRequest());

        assertThat(entity.getActorId()).isEqualTo("CIF-99001");
        assertThat(entity.getCustomerId()).isEqualTo("CIF-99001");
        assertThat(entity.getBusinessRef()).isEqualTo("IMPS1A2B3C4D");
        assertThat(entity.getAmount()).isEqualByComparingTo("82450.00");
    }

    @Test
    void valuesAlreadyMaskedByTheStarterAreStoredUnchanged() {

        AuditLog once = mapper.toEntity(rawRequest());

        AuditIngestRequest alreadyMasked = rawRequest();
        alreadyMasked.setMobileNumber(once.getMobileNumber());
        alreadyMasked.setApiEndpoint(once.getApiEndpoint());
        alreadyMasked.setBusinessContext(once.getBusinessContext());
        alreadyMasked.setBeforeState(once.getBeforeState());
        alreadyMasked.setAfterState(once.getAfterState());

        AuditLog twice = mapper.toEntity(alreadyMasked);

        assertThat(twice.getMobileNumber()).isEqualTo(once.getMobileNumber());
        assertThat(twice.getApiEndpoint()).isEqualTo(once.getApiEndpoint());
        assertThat(twice.getBusinessContext()).isEqualTo(once.getBusinessContext());
        assertThat(twice.getBeforeState()).isEqualTo(once.getBeforeState());
        assertThat(twice.getAfterState()).isEqualTo(once.getAfterState());
    }

    private static AuditIngestRequest rawRequest() {

        AuditIngestRequest request = new AuditIngestRequest();

        request.setTraceId("TRACE-1");
        request.setBankCode("NPST");
        request.setEnvironment("DEV");
        request.setService("nestjs-bill-payment");
        request.setActorId("CIF-99001");
        request.setActorType("CUSTOMER");
        request.setCustomerId("CIF-99001");
        request.setMobileNumber("9876543210");
        request.setAction("LIMIT_CHANGE");
        request.setEntity("ACCOUNT");
        request.setApiEndpoint("/api/v1/accounts/918273645510/limit");
        request.setBusinessRef("IMPS1A2B3C4D");
        request.setAmount(new BigDecimal("82450.00"));
        request.setCurrency("INR");
        request.setBusinessContext(Map.of("beneficiaryAccount", "918273645599"));
        request.setBeforeState(Map.of("email", "rajesh.amin@bharatbank.in"));
        request.setAfterState(Map.of(
                "accountNumber", "918273645510",
                "beneficiaryName", "Rajesh Amin",
                "status", "ACTIVE"));
        request.setTimestamp(Instant.parse("2026-09-14T10:00:00Z"));

        return request;
    }
}
