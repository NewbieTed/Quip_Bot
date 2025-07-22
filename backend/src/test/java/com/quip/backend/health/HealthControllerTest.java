package com.quip.backend.health;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HealthController}.
 * 
 * This test class validates the health controller functionality including
 * health check and root endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Tests")
class HealthControllerTest extends BaseTest {

    @InjectMocks
    private HealthController healthController;

    @Test
    @DisplayName("Should instantiate controller successfully")
    void shouldInstantiateController_Successfully() {
        // When & Then
        assertThat(healthController).isNotNull();
    }

    /**
     * Tests for the health endpoint which provides a basic health check.
     * This nested class validates that the endpoint returns the expected response.
     */
    @Nested
    @DisplayName("health Endpoint Tests")
    class HealthEndpointTests {

        @Test
        @DisplayName("Should return OK status")
        void shouldReturnOkStatus() {
            // When
            String response = healthController.health();

            // Then
            assertThat(response).isNotNull();
            assertThat(response).isEqualTo("OK");
        }
    }

    /**
     * Tests for the root endpoint which provides basic connectivity testing.
     * This nested class validates that the endpoint returns the expected response.
     */
    @Nested
    @DisplayName("root Endpoint Tests")
    class RootEndpointTests {

        @Test
        @DisplayName("Should return service running message")
        void shouldReturnServiceRunningMessage() {
            // When
            String response = healthController.root();

            // Then
            assertThat(response).isNotNull();
            assertThat(response).isEqualTo("Quip Backend Service is running");
        }
    }
}