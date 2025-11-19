package com.eaglebank.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class for the Eagle Bank API Spring Boot application.
 * This class bootstraps the application and starts the embedded Tomcat server.
 */
@SpringBootApplication
public class EagleBankApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(EagleBankApiApplication.class, args);
    }
}