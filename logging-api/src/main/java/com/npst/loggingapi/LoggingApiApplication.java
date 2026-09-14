package com.npst.loggingapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// central service that stores and searches logs and audit records
@SpringBootApplication
@EnableScheduling
public class LoggingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoggingApiApplication.class, args);
    }
}