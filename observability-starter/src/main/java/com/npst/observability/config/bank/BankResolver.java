package com.npst.observability.config.bank;

// tells the logger which bank, environment and service it is running in
public interface BankResolver {

    String getCode();

    String getName();

    String getRegion();

    String getEnvironment();

    String getServiceName();
}
