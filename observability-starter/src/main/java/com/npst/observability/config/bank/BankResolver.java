package com.npst.observability.config.bank;

/**
 * Supplies the identity stamped onto every log line: which bank, which
 * environment, which service.
 *
 * <p>An interface rather than a class so a deployment with a different notion
 * of identity can replace it without forking the starter - the
 * auto-configuration only contributes {@link PropertyBankResolver} when the
 * application has not defined a BankResolver of its own.
 */
public interface BankResolver {

    /** Short code stamped on every log, e.g. NPST. */
    String getCode();

    /** Human readable institution name. */
    String getName();

    /** Deployment region, e.g. IN. */
    String getRegion();

    /** Deployment environment: DEV, UAT, PROD. */
    String getEnvironment();

    /** Logical name of the emitting microservice. */
    String getServiceName();
}
