package com.quip.backend.config;

import com.quip.backend.config.redis.RedisCacheProperties;
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
 * Unit tests for RedisCacheProperties configuration class.
 */
class RedisCachePropertiesTest {

    private RedisCacheProperties cacheProperties;
    private Validator validator;

    @BeforeEach
    void setUp() {
        cacheProperties = new RedisCacheProperties();
        cacheProperties.setTimeToLive(Duration.ofMillis(7200000)); // 2 hours
        cacheProperties.setCacheNullValues(true);
        cacheProperties.setKeyPrefix("test:app:");

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testPropertiesSetup() {
        assertEquals(Duration.ofMillis(7200000), cacheProperties.getTimeToLive());
        assertTrue(cacheProperties.isCacheNullValues());
        assertEquals("test:app:", cacheProperties.getKeyPrefix());
    }

    @Test
    void testTimeToLiveConversions() {
        assertEquals(7200000L, cacheProperties.getTimeToLiveMillis());
        assertEquals(7200L, cacheProperties.getTimeToLiveSeconds());
    }

    @Test
    void testValidation() {
        Set<ConstraintViolation<RedisCacheProperties>> violations = validator.validate(cacheProperties);
        assertTrue(violations.isEmpty(), "Properties should be valid");
    }

    @Test
    void testInvalidTimeToLive() {
        cacheProperties.setTimeToLive(Duration.ZERO);
        Set<ConstraintViolation<RedisCacheProperties>> violations = validator.validate(cacheProperties);
        assertFalse(violations.isEmpty());
        
        cacheProperties.setTimeToLive(Duration.ofMillis(-1000));
        violations = validator.validate(cacheProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidKeyPrefix() {
        cacheProperties.setKeyPrefix("");
        Set<ConstraintViolation<RedisCacheProperties>> violations = validator.validate(cacheProperties);
        assertFalse(violations.isEmpty());
        
        cacheProperties.setKeyPrefix("   ");
        violations = validator.validate(cacheProperties);
        assertFalse(violations.isEmpty());
    }
}