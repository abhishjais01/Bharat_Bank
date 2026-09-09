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
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What {@code @LogRegistry} must guarantee before it goes near a payment path:
 * it records the right things, and it changes nothing about the method it
 * wraps.
 */
class LogRegistryAspectTest {

    private final List<LogIngestRequest> shipped = new CopyOnWriteArrayList<>();

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

    /**
     * The reason arguments are converted to a map before logging. Left as an
     * object, a request would be written via toString() with its OTP intact -
     * the masker only recognises keys.
     */
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
                new ObjectMapper(), null, request -> { }, null,
                new ObservabilityProperties()) {
        });

        // BankResolver and masker are null, so recording blows up internally.
        // The transfer must still succeed.
        assertThat(fragile.transfer("TXN-005")).isEqualTo("processed TXN-005");
    }

    // ---------------------------------------------------------------- setup

    private LogIngestRequest last() {
        assertThat(shipped).isNotEmpty();
        return shipped.get(shipped.size() - 1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lastMetadata() {
        return (Map<String, Object>) last().getMetadata();
    }

    private BankingService proxy() {

        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getBank().setCode("NPST");

        LogSink capturing = shipped::add;

        CommonLogger logger = new CommonLoggerImpl(
                new ObjectMapper(),
                new StubResolver(),
                capturing,
                new MetadataMasker(properties.getMasking()),
                properties);

        return proxyWith(logger);
    }

    private BankingService proxyWith(CommonLogger logger) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new BankingService());
        factory.addAspect(new LogRegistryAspect(logger, new ObjectMapper()));
        return factory.getProxy();
    }

    /** Stands in for a real banking service. */
    static class BankingService {

        @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", entity = "TRANSFER")
        public String transfer(String reference) {
            return "processed " + reference;
        }

        @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS")
        public String failingTransfer() {
            throw new IllegalStateException("insufficient funds");
        }

        @LogRegistry(action = "ADD_BENEFICIARY", module = "PAYMENTS", logArguments = true)
        public String addBeneficiary(BeneficiaryRequest request) {
            return "added";
        }
    }

    /** Deliberately holds an OTP, as the real US-09 request does. */
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
