package com.bank.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A stand-in for a real Bharat Bank microservice.
 *
 * <p>The package is {@code com.bank.mock}, deliberately unrelated to
 * {@code com.npst.observability}. That is the point: it proves the starter is
 * discovered through auto-configuration rather than through a shared component
 * scan, which is what makes it usable by services this team does not own.
 *
 * <p>Replacing this with a real service is a dependency, three YAML lines and
 * the annotations - no logging code in the handlers themselves.
 */
@SpringBootApplication
public class SampleBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleBankApplication.class, args);
    }
}
