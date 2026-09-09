package com.npst.loggingapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoggingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoggingApiApplication.class, args);
    }
}