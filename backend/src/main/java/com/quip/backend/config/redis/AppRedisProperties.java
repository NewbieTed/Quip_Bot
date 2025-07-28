package com.quip.backend.config.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

/**
 * Application-specific Redis configuration properties.
 * Supports externalized configuration with validation and environment variable overrides.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {

    /**
     * Default TTL for application cache entries in seconds
     */
    @Min(value = 1, message = "Default TTL must be at least 1 second")
    private long defaultTtl = 3600; // 1 hour

    /**
     * Key prefix for application-specific cache entries
     */
    @NotBlank(message = "Application key prefix cannot be blank")
    private String keyPrefix = "quip:backend:";

    /**
     * Health check interval in milliseconds
     */
    @Min(value = 1000, message = "Health check interval must be at least 1000ms")
    private long healthCheckInterval = 30000; // 30 seconds

    /**
     * Maximum retry attempts for Redis operations
     */
    @Min(value = 0, message = "Max retry attempts must be non-negative")
    private int maxRetryAttempts = 3;

    /**
     * Retry delay between attempts in milliseconds
     */
    @Min(value = 100, message = "Retry delay must be at least 100ms")
    private long retryDelay = 1000; // 1 second

    /**
     * Circuit breaker failure threshold
     */
    @Min(value = 1, message = "Failure threshold must be at least 1")
    private int failureThreshold = 5;

    /**
     * Circuit breaker recovery timeout in milliseconds
     */
    @Min(value = 1000, message = "Recovery timeout must be at least 1000ms")
    private long recoveryTimeout = 60000; // 1 minute

    /**
     * Gets the default TTL as Duration
     * 
     * @return default TTL as Duration
     */
    public Duration getDefaultTtlDuration() {
        return Duration.ofSeconds(defaultTtl);
    }

    /**
     * Gets the health check interval as Duration
     * 
     * @return health check interval as Duration
     */
    public Duration getHealthCheckIntervalDuration() {
        return Duration.ofMillis(healthCheckInterval);
    }

    /**
     * Gets the retry delay as Duration
     * 
     * @return retry delay as Duration
     */
    public Duration getRetryDelayDuration() {
        return Duration.ofMillis(retryDelay);
    }

    /**
     * Gets the recovery timeout as Duration
     * 
     * @return recovery timeout as Duration
     */
    public Duration getRecoveryTimeoutDuration() {
        return Duration.ofMillis(recoveryTimeout);
    }

    /**
     * Validates that retry delay is reasonable compared to health check interval
     */
    @AssertTrue(message = "Retry delay should be less than health check interval")
    public boolean isValidRetryDelay() {
        return retryDelay < healthCheckInterval;
    }

    /**
     * Validates that recovery timeout is reasonable
     */
    @AssertTrue(message = "Recovery timeout should be greater than retry delay")
    public boolean isValidRecoveryTimeout() {
        return recoveryTimeout > retryDelay;
    }
}