package com.bank.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// mock bank service used to try out the logging platform
@SpringBootApplication
public class SampleBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleBankApplication.class, args);
    }
}
