package com.quip.backend.config;

import com.quip.backend.config.redis.AppRedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppRedisProperties configuration class.
 */
class AppRedisPropertiesTest {

    private AppRedisProperties appRedisProperties;
    private Validator validator;

    @BeforeEach
    void setUp() {
        appRedisProperties = new AppRedisProperties();
        appRedisProperties.setDefaultTtl(7200);
        appRedisProperties.setKeyPrefix("test:backend:");
        appRedisProperties.setHealthCheckInterval(60000);
        appRedisProperties.setMaxRetryAttempts(5);
        appRedisProperties.setRetryDelay(2000);
        appRedisProperties.setFailureThreshold(10);
        appRedisProperties.setRecoveryTimeout(120000);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testPropertiesSetup() {
        assertEquals(7200L, appRedisProperties.getDefaultTtl());
        assertEquals("test:backend:", appRedisProperties.getKeyPrefix());
        assertEquals(60000L, appRedisProperties.getHealthCheckInterval());
        assertEquals(5, appRedisProperties.getMaxRetryAttempts());
        assertEquals(2000L, appRedisProperties.getRetryDelay());
        assertEquals(10, appRedisProperties.getFailureThreshold());
        assertEquals(120000L, appRedisProperties.getRecoveryTimeout());
    }

    @Test
    void testDurationConversions() {
        assertEquals(Duration.ofSeconds(7200), appRedisProperties.getDefaultTtlDuration());
        assertEquals(Duration.ofMillis(60000), appRedisProperties.getHealthCheckIntervalDuration());
        assertEquals(Duration.ofMillis(2000), appRedisProperties.getRetryDelayDuration());
        assertEquals(Duration.ofMillis(120000), appRedisProperties.getRecoveryTimeoutDuration());
    }

    @Test
    void testValidation() {
        Set<ConstraintViolation<AppRedisProperties>> violations = validator.validate(appRedisProperties);
        assertTrue(violations.isEmpty(), "Properties should be valid");
    }

    @Test
    void testInvalidDefaultTtl() {
        appRedisProperties.setDefaultTtl(0);
        Set<ConstraintViolation<AppRedisProperties>> violations = validator.validate(appRedisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidHealthCheckInterval() {
        appRedisProperties.setHealthCheckInterval(500);
        Set<ConstraintViolation<AppRedisProperties>> violations = validator.validate(appRedisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidRetryDelay() {
        appRedisProperties.setRetryDelay(50);
        Set<ConstraintViolation<AppRedisProperties>> violations = validator.validate(appRedisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRetryDelayValidation() {
        // Set retry delay greater than health check interval
        appRedisProperties.setRetryDelay(70000);
        appRedisProperties.setHealthCheckInterval(60000);
        
        Set<ConstraintViolation<AppRedisProperties>> violations = validator.validate(appRedisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRecoveryTimeoutValidation() {
        // Set recovery timeout less than retry delay
        appRedisProperties.setRecoveryTimeout(1000);
        appRedisProperties.setRetryDelay(2000);
        
        Set<ConstraintViolation<AppRedisProperties>> violations = validator.validate(appRedisProperties);
        assertFalse(violations.isEmpty());
    }
}