package com.quip.backend.config.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for Redis cache settings.
 * Supports externalized configuration with validation and environment variable overrides.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "spring.cache.redis")
public class RedisCacheProperties {

    /**
     * Default time-to-live for cache entries
     */
    @NotNull(message = "Cache TTL cannot be null")
    private Duration timeToLive = Duration.ofHours(1);

    /**
     * Whether to cache null values
     */
    private boolean cacheNullValues = false;

    /**
     * Key prefix for all cache entries
     */
    @NotBlank(message = "Cache key prefix cannot be blank")
    private String keyPrefix = "quip:backend:";

    /**
     * Validates that TTL is positive
     */
    @AssertTrue(message = "Cache TTL must be positive")
    public boolean isValidTimeToLive() {
        return timeToLive != null && !timeToLive.isNegative() && !timeToLive.isZero();
    }

    /**
     * Gets the TTL in milliseconds
     * 
     * @return TTL in milliseconds
     */
    public long getTimeToLiveMillis() {
        return timeToLive.toMillis();
    }

    /**
     * Gets the TTL in seconds
     * 
     * @return TTL in seconds
     */
    public long getTimeToLiveSeconds() {
        return timeToLive.getSeconds();
    }
}