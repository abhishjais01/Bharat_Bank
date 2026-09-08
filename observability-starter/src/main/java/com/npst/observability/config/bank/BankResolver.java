package com.npst.observability.config.bank;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BankResolver {

    private static final String UNKNOWN_SERVICE = "unknown-service";

    private final BankProperties properties;
    private final Environment springEnvironment;

    public BankResolver(BankProperties properties, Environment springEnvironment) {
        this.properties = properties;
        this.springEnvironment = springEnvironment;
    }

    public String getCode() {
        return properties.getCode();
    }

    public String getName() {
        return properties.getName();
    }

    public String getEnvironment() {
        return properties.getEnvironment();
    }

    public String getRegion() {
        return properties.getRegion();
    }

    public String getServiceName() {

        String configured = properties.getServiceName();

        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        return springEnvironment.getProperty("spring.application.name", UNKNOWN_SERVICE);
    }
}
