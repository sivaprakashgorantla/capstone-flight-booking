package com.capstone.runner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ServicesRunnerApplication {

    public static void main(String[] args) throws IOException {

        List<String> services = Arrays.asList(
                "eureka-server",
                "api-gateway",
                "auth-service",
                "user-service",
                "flight-service",
                "booking-service",
                "payment-service",
                "cancellation-service",
                "support-service",
                "notification-service",
                "analytics-reporting-service"
        );

        for (String service : services) {

            ProcessBuilder builder = new ProcessBuilder(
                    "cmd",
                    "/c",
                    "start",
                    "cmd",
                    "/k",
                    "mvn spring-boot:run -pl " + service
            );

            builder.inheritIO();
            builder.start();
        }

        System.out.println("All Microservices Started...");
    }
}