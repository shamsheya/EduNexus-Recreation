package com.edunexus;

import org.springframework.boot.SpringApplication;

/**
 * Local development entry point that runs the real application on the test classpath, so the
 * embedded MongoDB (flapdoodle) is started automatically. Usage:
 *   ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.demo-data=true
 */
public class TestEduNexusApplication {
    public static void main(String[] args) {
        SpringApplication.from(EduNexusApplication::main).run(args);
    }
}
