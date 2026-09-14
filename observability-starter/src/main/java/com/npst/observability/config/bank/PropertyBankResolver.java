package com.npst.observability.config.bank;

import com.npst.observability.config.ObservabilityProperties;
import org.springframework.core.env.Environment;

// default resolver: reads the values from observability.* config
public class PropertyBankResolver implements BankResolver {

    private static final String UNKNOWN_SERVICE = "unknown-service";

    private final ObservabilityProperties properties;
    private final Environment springEnvironment;

    public PropertyBankResolver(ObservabilityProperties properties,
                                Environment springEnvironment) {
        this.properties = properties;
        this.springEnvironment = springEnvironment;
    }

    @Override
    public String getCode() {
        return properties.getBank().getCode();
    }

    @Override
    public String getName() {
        return properties.getBank().getName();
    }

    @Override
    public String getRegion() {
        return properties.getBank().getRegion();
    }

    @Override
    public String getEnvironment() {
        return properties.getEnvironment();
    }

    // service name from config, or spring.application.name
    @Override
    public String getServiceName() {

        String configured = properties.getService();

        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        return springEnvironment.getProperty("spring.application.name", UNKNOWN_SERVICE);
    }
}
