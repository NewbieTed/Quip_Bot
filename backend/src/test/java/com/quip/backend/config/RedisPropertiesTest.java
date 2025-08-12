package com.quip.backend.config;

import com.quip.backend.config.redis.RedisProperties;
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
 * Unit tests for RedisProperties configuration class.
 */
class RedisPropertiesTest {

    private RedisProperties redisProperties;
    private Validator validator;

    @BeforeEach
    void setUp() {
        redisProperties = new RedisProperties();
        redisProperties.setHost("test-host");
        redisProperties.setPort(6380);
        redisProperties.setPassword("test-password");
        redisProperties.setDatabase(1);
        redisProperties.setTimeout(Duration.ofMillis(5000));
        
        RedisProperties.Lettuce.Pool pool = redisProperties.getLettuce().getPool();
        pool.setMaxActive(10);
        pool.setMaxIdle(5);
        pool.setMinIdle(2);
        pool.setMaxWait(Duration.ofMillis(2000));

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testPropertiesSetup() {
        assertEquals("test-host", redisProperties.getHost());
        assertEquals(6380, redisProperties.getPort());
        assertEquals("test-password", redisProperties.getPassword());
        assertEquals(1, redisProperties.getDatabase());
        assertEquals(Duration.ofMillis(5000), redisProperties.getTimeout());
        
        RedisProperties.Lettuce.Pool pool = redisProperties.getLettuce().getPool();
        assertEquals(10, pool.getMaxActive());
        assertEquals(5, pool.getMaxIdle());
        assertEquals(2, pool.getMinIdle());
        assertEquals(Duration.ofMillis(2000), pool.getMaxWait());
    }

    @Test
    void testPasswordEnabled() {
        assertTrue(redisProperties.isPasswordEnabled());
        
        redisProperties.setPassword("");
        assertFalse(redisProperties.isPasswordEnabled());
        
        redisProperties.setPassword("   ");
        assertFalse(redisProperties.isPasswordEnabled());
    }

    @Test
    void testConnectionUrl() {
        String expectedUrl = "redis://test-host:6380/1";
        assertEquals(expectedUrl, redisProperties.getConnectionUrl());
    }

    @Test
    void testValidation() {
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(redisProperties);
        assertTrue(violations.isEmpty(), "Properties should be valid");
    }

    @Test
    void testInvalidPort() {
        redisProperties.setPort(0);
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
        
        redisProperties.setPort(70000);
        violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidDatabase() {
        redisProperties.setDatabase(-1);
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
        
        redisProperties.setDatabase(16);
        violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidPoolConfiguration() {
        RedisProperties.Lettuce.Pool pool = redisProperties.getLettuce().getPool();
        pool.setMaxIdle(15);
        pool.setMaxActive(10);
        
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidTimeout() {
        redisProperties.setTimeout(Duration.ZERO);
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
        
        redisProperties.setTimeout(Duration.ofMillis(-1000));
        violations = validator.validate(redisProperties);
        assertFalse(violations.isEmpty());
    }
}