package com.npst.observability.config.bank;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "observability.bank")
public class BankProperties {

    private String code;
    private String name;
    private String environment;
    private String region;

    /**
     * Logical name of the service emitting the log. When left blank the
     * resolver falls back to {@code spring.application.name}, so a service
     * importing the starter needs no extra configuration.
     */
    private String serviceName;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
