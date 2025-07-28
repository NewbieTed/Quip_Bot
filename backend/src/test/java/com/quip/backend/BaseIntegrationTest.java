package com.quip.backend;

import com.quip.backend.config.TestRedisConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for integration tests that ensures proper test configuration
 * and prevents actual database connections.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestRedisConfig.class})
public abstract class BaseIntegrationTest {
    // Base class for integration tests with proper Redis mocking
}