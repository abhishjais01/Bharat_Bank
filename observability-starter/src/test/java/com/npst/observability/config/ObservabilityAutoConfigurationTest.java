package com.npst.observability.config;

import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.contract.LogIngestRequest;
import com.npst.observability.contract.LogLevel;
import com.npst.observability.logger.CommonLogger;
import com.npst.observability.schema.LogEvent;
import com.npst.observability.mapper.LogEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the two promises the auto-configuration makes to an importing
 * service: it can be switched off entirely, and any single bean can be
 * replaced without forking the starter.
 */
class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void contributesACommonLoggerByDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(CommonLogger.class));
    }

    @Test
    void contributesNothingWhenDisabled() {
        runner.withPropertyValues("observability.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CommonLogger.class));
    }

    @Test
    void applicationCanReplaceAnySingleBean() {
        runner.withUserConfiguration(CustomResolverConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(BankResolver.class);
                    assertThat(context.getBean(BankResolver.class).getCode())
                            .isEqualTo("OVERRIDDEN");
                });
    }

    @Test
    void timestampIsSerializedAsIso8601NotAnEpochNumber() throws Exception {
        // logging-api parses this field as an Instant. A mapper without
        // JavaTimeModule would emit 1757342309.815 and every log would be
        // rejected at the boundary.
        LogEvent event = new LogEvent();
        event.setTimestamp(Instant.parse("2026-09-08T14:38:29Z"));
        event.setLevel(LogLevel.INFO);
        event.setMessage("Balance fetched successfully");

        LogIngestRequest request = LogEventMapper.toIngestRequest(event);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        assertThat(mapper.writeValueAsString(request))
                .contains("\"timestamp\":\"2026-09-08T14:38:29Z\"")
                .contains("\"schemaVersion\":\"1.0\"");
    }

    @Configuration
    static class CustomResolverConfig {

        @Bean
        BankResolver bankResolver() {
            return new BankResolver() {
                @Override
                public String getCode() {
                    return "OVERRIDDEN";
                }

                @Override
                public String getName() {
                    return "Custom";
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
                    return "custom-service";
                }
            };
        }
    }
}
