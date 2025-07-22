package com.quip.backend.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health check controller for monitoring backend status.
 */
@RestController
public class HealthController {

    /**
     * Basic health check endpoint.
     * @return Simple status message
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    /**
     * Root endpoint for basic connectivity testing.
     * @return Simple status message
     */
    @GetMapping("/")
    public String root() {
        return "Quip Backend Service is running";
    }
}