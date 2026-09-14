package com.example.foreignbank;

import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.logger.CommonLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// checks the starter works in an app from a completely different package
@SpringBootTest(
        classes = ForeignPackageStarterTest.ForeignBankApplication.class,
        properties = {
                "spring.application.name=foreign-bank-service",
                "observability.environment=UAT",
                "observability.bank.code=NPST",
                "observability.bank.name=Bharat Bank",
                "observability.bank.region=IN",
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
        commonLogger.logApplication("Balance fetched successfully",
                java.util.Map.of("channel", "MOBILE", "module", "BALANCE"));
    }

    @SpringBootApplication
    static class ForeignBankApplication {
    }
}
