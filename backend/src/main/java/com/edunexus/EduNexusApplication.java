package com.edunexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// Authentication is JWT-only; no in-memory default user should be created.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableMongoAuditing
public class EduNexusApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduNexusApplication.class, args);
    }
}
