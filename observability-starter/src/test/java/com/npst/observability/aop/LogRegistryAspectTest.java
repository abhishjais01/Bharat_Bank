package com.npst.observability.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.contract.LogIngestRequest;
import com.npst.observability.contract.LogLevel;
import com.npst.observability.context.RequestContext;
import com.npst.observability.logger.CommonLogger;
import com.npst.observability.logger.CommonLoggerImpl;
import com.npst.observability.masking.MetadataMasker;
import com.npst.observability.sink.LogSink;
import com.npst.observability.sink.RecordingLogSink;
import com.npst.observability.contract.AuditIngestRequest;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// checks @LogRegistry logs and audits correctly without changing the method's result
class LogRegistryAspectTest {

    private final RecordingLogSink sink = new RecordingLogSink();

    private final BankingService service = proxy();

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void capturesTheConstantHalfFromTheAnnotation() {

        service.transfer("TXN-001");

        Map<String, Object> metadata = lastMetadata();

        assertThat(metadata)
                .containsEntry("action", "FUND_TRANSFER")
                .containsEntry("module", "PAYMENTS")
                .containsEntry("entity", "TRANSFER");
    }

    @Test
    void capturesTheRuntimeHalfFromTheExecution() {

        service.transfer("TXN-002");

        Map<String, Object> metadata = lastMetadata();

        assertThat(metadata)
                .containsEntry("outcome", "SUCCESS")
                .containsKey("durationMs")
                .containsKey("operation");

        assertThat(last().getMessage()).isEqualTo("FUND_TRANSFER completed");
        assertThat(last().getLevel()).isEqualTo(LogLevel.INFO);
    }

    @Test
    void aFailureIsRecordedAtErrorAndTheExceptionStillReachesTheCaller() {

        assertThatThrownBy(() -> service.failingTransfer())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("insufficient funds");

        assertThat(last().getLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(last().getMessage()).isEqualTo("FUND_TRANSFER failed");

        assertThat(lastMetadata())
                .containsEntry("outcome", "FAILURE")
                .containsEntry("exception", "java.lang.IllegalStateException")
                .containsEntry("responseMessage", "insufficient funds");
    }

    @Test
    void theReturnValueIsPassedThroughUntouched() {
        assertThat(service.transfer("TXN-003")).isEqualTo("processed TXN-003");
    }

    @Test
    void sensitiveArgumentFieldsAreMaskedNotPrinted() {

        service.addBeneficiary(new BeneficiaryRequest("918273645510", "483920"));

        String written = lastMetadata().toString();

        assertThat(written)
                .doesNotContain("483920")
                .doesNotContain("918273645510")
                .contains("***REDACTED***")
                .contains("XXXXXXXX5510");
    }

    @Test
    void carriesTheRequestContextCapturedAtTheEdge() {

        RequestContext.put(RequestContext.CHANNEL, "MOBILE");
        RequestContext.put(RequestContext.DEVICE_ID, "device-42");
        RequestContext.put(RequestContext.CUSTOMER_ID, "CIF-99001");
        RequestContext.put(RequestContext.IP_ADDRESS, "10.1.2.3");

        service.transfer("TXN-004");

        assertThat(last().getChannel()).isEqualTo("MOBILE");
        assertThat(last().getDeviceId()).isEqualTo("device-42");
        assertThat(last().getCustomerId()).isEqualTo("CIF-99001");
        assertThat(last().getIpAddress()).isEqualTo("10.1.2.3");
    }

    @Test
    void aBrokenLoggerNeverBreaksTheBankingCall() {

        BankingService fragile = proxyWith(new CommonLoggerImpl(
                new ObjectMapper(), null, new RecordingLogSink(), null,
                new ObservabilityProperties()));

        assertThat(fragile.transfer("TXN-005")).isEqualTo("processed TXN-005");
    }

    @Test
    void anAuditedActionProducesAnAuditRecordWithTheRuntimeDetail() {

        RequestContext.put(RequestContext.CUSTOMER_ID, "CIF-99001");
        RequestContext.put(RequestContext.CHANNEL, "MOBILE");

        service.auditedTransfer("TXN-100");

        assertThat(sink.audits).hasSize(1);

        AuditIngestRequest audit = sink.lastAudit();

        assertThat(audit.getAction()).isEqualTo("FUND_TRANSFER");
        assertThat(audit.getModule()).isEqualTo("PAYMENTS");
        assertThat(audit.getEntity()).isEqualTo("TRANSFER");

        assertThat(audit.getActorId()).isEqualTo("CIF-99001");
        assertThat(audit.getActorType()).isEqualTo("CUSTOMER");
        assertThat(audit.getChannel()).isEqualTo("MOBILE");
        assertThat(audit.getCustomerId()).isEqualTo("CIF-99001");
        assertThat(audit.getDurationMs()).isNotNull();
        assertThat(audit.getDescription()).isEqualTo("FUND_TRANSFER completed");
    }

    @Test
    void anAuditRecordMasksPrivateCustomerDetailsButKeepsTheActor() {

        RequestContext.put(RequestContext.CUSTOMER_ID, "CIF-99001");

        service.auditedBeneficiary();

        AuditIngestRequest audit = sink.lastAudit();

        assertThat(audit.getActorId()).isEqualTo("CIF-99001");
        assertThat(audit.getCustomerId()).isEqualTo("CIF-99001");
        assertThat(audit.getMobileNumber()).isEqualTo("98XXXX3210");
        assertThat(audit.getBusinessContext()).containsEntry("beneficiaryAccount", "XXXXXXXX5599");
        assertThat(audit.getAfterState())
                .containsEntry("accountNumber", "XXXXXXXX5510")
                .containsEntry("beneficiaryName", "RXXXXX AXXX");
    }

    @Test
    void anUnauditedActionProducesNoAuditRecord() {
        service.transfer("TXN-101");

        assertThat(sink.audits).isEmpty();
        assertThat(sink.logs).isNotEmpty();
    }

    @Test
    void withNoCustomerInContextTheActorIsSystem() {

        service.auditedTransfer("TXN-102");

        assertThat(sink.lastAudit().getActorId()).isEqualTo("SYSTEM");
        assertThat(sink.lastAudit().getActorType()).isEqualTo("SYSTEM");
    }

    private LogIngestRequest last() {
        assertThat(sink.logs).isNotEmpty();
        return sink.lastLog();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lastMetadata() {
        return (Map<String, Object>) last().getMetadata();
    }

    private BankingService proxy() {

        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getBank().setCode("NPST");

        CommonLogger logger = new CommonLoggerImpl(
                new ObjectMapper(),
                new StubResolver(),
                sink,
                new MetadataMasker(properties.getMasking()),
                properties);

        return proxyWith(logger);
    }

    private BankingService proxyWith(CommonLogger logger) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new BankingService());
        factory.addAspect(new LogRegistryAspect(logger, new ObjectMapper()));
        return factory.getProxy();
    }

    static class BankingService {

        @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", entity = "TRANSFER")
        public String transfer(String reference) {
            return "processed " + reference;
        }

        @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS")
        public String failingTransfer() {
            throw new IllegalStateException("insufficient funds");
        }

        @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS",
                entity = "TRANSFER", audit = true)
        public String auditedTransfer(String reference) {
            return "processed " + reference;
        }

        @LogRegistry(action = "ADD_BENEFICIARY", module = "PAYMENTS", logArguments = true)
        public String addBeneficiary(BeneficiaryRequest request) {
            return "added";
        }

        @LogRegistry(action = "ADD_BENEFICIARY", module = "PAYMENTS",
                entity = "BENEFICIARY", audit = true)
        public String auditedBeneficiary() {
            com.npst.observability.context.AuditContext.mobileNumber("9876543210");
            com.npst.observability.context.AuditContext.put("beneficiaryAccount", "918273645599");
            com.npst.observability.context.AuditContext.afterState(Map.of(
                    "beneficiaryName", "Rajesh Amin",
                    "accountNumber", "918273645510"));
            return "added";
        }
    }

    record BeneficiaryRequest(String accountNumber, String otp) {
    }

    static class StubResolver implements com.npst.observability.config.bank.BankResolver {

        @Override
        public String getCode() {
            return "NPST";
        }

        @Override
        public String getName() {
            return "Bharat Bank";
        }

        @Override
        public String getRegion() {
            return "IN";
        }

        @Override
        public String getEnvironment() {
            return "TEST";
        }

        @Override
        public String getServiceName() {
            return "test-service";
        }
    }

    @Aspect
    static class Unused {
    }
}
