package com.quip.backend.config.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;

/**
 * Configuration properties for Redis connection and caching settings.
 * Supports externalized configuration with validation and environment variable overrides.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    /**
     * Whether Redis is enabled for the application
     */
    private boolean enabled = true;

    /**
     * Redis server hostname
     */
    @NotBlank(message = "Redis host cannot be blank")
    private String host = "localhost";

    /**
     * Redis server port
     */
    @Min(value = 1, message = "Redis port must be greater than 0")
    @Max(value = 65535, message = "Redis port must be less than or equal to 65535")
    private int port = 6379;

    /**
     * Redis server password (optional)
     */
    private String password = "";

    /**
     * Redis database index
     */
    @Min(value = 0, message = "Redis database index must be non-negative")
    @Max(value = 15, message = "Redis database index must be less than or equal to 15")
    private int database = 0;

    /**
     * Connection timeout
     */
    @NotNull(message = "Redis timeout cannot be null")
    private Duration timeout = Duration.ofMillis(2000);

    /**
     * Lettuce connection pool configuration
     */
    @Valid
    private Lettuce lettuce = new Lettuce();

    /**
     * Lettuce-specific configuration properties
     */
    @Data
    public static class Lettuce {
        
        /**
         * Connection pool configuration
         */
        @Valid
        private Pool pool = new Pool();

        /**
         * Connection pool configuration properties
         */
        @Data
        public static class Pool {
            
            /**
             * Maximum number of active connections in the pool
             */
            @Min(value = 1, message = "Max active connections must be at least 1")
            private int maxActive = 8;

            /**
             * Maximum number of idle connections in the pool
             */
            @Min(value = 0, message = "Max idle connections must be non-negative")
            private int maxIdle = 8;

            /**
             * Minimum number of idle connections in the pool
             */
            @Min(value = 0, message = "Min idle connections must be non-negative")
            private int minIdle = 0;

            /**
             * Maximum time to wait for a connection from the pool
             * Use negative value for no timeout
             */
            @NotNull(message = "Max wait duration cannot be null")
            private Duration maxWait = Duration.ofMillis(-1);
        }
    }

    /**
     * Validates that max idle is not greater than max active connections
     */
    @AssertTrue(message = "Max idle connections cannot be greater than max active connections")
    public boolean isValidPoolConfiguration() {
        return lettuce.pool.maxIdle <= lettuce.pool.maxActive;
    }

    /**
     * Validates that min idle is not greater than max idle connections
     */
    @AssertTrue(message = "Min idle connections cannot be greater than max idle connections")
    public boolean isValidMinIdleConfiguration() {
        return lettuce.pool.minIdle <= lettuce.pool.maxIdle;
    }

    /**
     * Validates that timeout is positive
     */
    @AssertTrue(message = "Redis timeout must be positive")
    public boolean isValidTimeout() {
        return timeout != null && !timeout.isNegative() && !timeout.isZero();
    }

    /**
     * Checks if password authentication is enabled
     * 
     * @return true if password is provided and not empty
     */
    public boolean isPasswordEnabled() {
        return password != null && !password.trim().isEmpty();
    }

    /**
     * Gets the connection URL for Redis
     * 
     * @return Redis connection URL
     */
    public String getConnectionUrl() {
        return String.format("redis://%s:%d/%d", host, port, database);
    }
}