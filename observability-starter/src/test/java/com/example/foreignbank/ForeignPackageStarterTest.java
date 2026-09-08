package com.example.foreignbank;

import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.logger.CommonLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The regression guard for the platform's whole reason to exist.
 *
 * <p>This test lives in {@code com.example.foreignbank} on purpose. A real
 * banking service will be packaged as {@code com.bank.accounts} or similar,
 * nowhere near {@code com.npst.observability}. Before auto-configuration was
 * introduced the SDK's beans were plain {@code @Component}s that only got
 * picked up because the sample application shared the starter's base package -
 * so the platform appeared to work while being unusable by any real service.
 *
 * <p>If this test ever fails, the starter has stopped being importable.
 */
@SpringBootTest(
        classes = ForeignPackageStarterTest.ForeignBankApplication.class,
        properties = {
                "spring.application.name=foreign-bank-service",
                "observability.environment=UAT",
                "observability.bank.code=NPST",
                "observability.bank.name=Bharat Bank",
                "observability.bank.region=IN",
                // Nothing to receive HTTP in a unit test.
                "observability.sink.enabled=false"
        })
class ForeignPackageStarterTest {

    @Autowired
    private CommonLogger commonLogger;

    @Autowired
    private BankResolver bankResolver;

    @Test
    void starterIsDiscoveredFromAForeignBasePackage() {
        assertThat(commonLogger)
                .as("CommonLogger must be contributed by auto-configuration, "
                        + "not by the host application's component scan")
                .isNotNull();
    }

    @Test
    void serviceNameDefaultsToSpringApplicationName() {
        // A new microservice should not have to name itself twice.
        assertThat(bankResolver.getServiceName()).isEqualTo("foreign-bank-service");
    }

    @Test
    void bankIdentityComesFromConfiguration() {
        assertThat(bankResolver.getCode()).isEqualTo("NPST");
        assertThat(bankResolver.getName()).isEqualTo("Bharat Bank");
        assertThat(bankResolver.getRegion()).isEqualTo("IN");
        assertThat(bankResolver.getEnvironment()).isEqualTo("UAT");
    }

    @Test
    void loggingASingleLineRequiresNoPlatformArgumentsFromTheCaller() {
        // Exactly what a developer in a banking service writes.
        commonLogger.logApplication("Balance fetched successfully",
                java.util.Map.of("channel", "MOBILE", "module", "BALANCE"));
    }

    @SpringBootApplication
    static class ForeignBankApplication {
    }
}
