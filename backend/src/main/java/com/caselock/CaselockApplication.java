package com.caselock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * CaseLock - Secure Digital Evidence Management and Chain-of-Custody System.
 *
 * <p>Entry point for the Spring Boot backend. The application follows a
 * layered architecture: Controller -> Service -> Repository -> Entity -> MySQL.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class CaselockApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaselockApplication.class, args);
    }
}
