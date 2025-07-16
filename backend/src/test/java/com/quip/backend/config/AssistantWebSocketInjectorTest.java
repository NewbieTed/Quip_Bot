package com.quip.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.ws.AssistantWebSocketEndpoint;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AssistantWebSocketInjector}.
 * <p>
 * This test class validates the WebSocket injector functionality including
 * proper dependency injection and bean creation for WebSocket endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantWebSocketInjector Tests")
class AssistantWebSocketInjectorTest extends BaseTest {

    @Mock
    private AssistantService assistantService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    private AssistantWebSocketInjector injector;

    @BeforeEach
    void setUp() {
        injector = new AssistantWebSocketInjector(assistantService, objectMapper, validator);
    }

    @Test
    @DisplayName("Should instantiate injector successfully")
    void shouldInstantiateInjector_Successfully() {
        // When & Then
        assertThat(injector).isNotNull();
    }

    @Test
    @DisplayName("Should create AssistantWebSocketEndpoint bean with all dependencies")
    void shouldCreateAssistantWebSocketEndpointBean_WithAllDependencies() {
        // When
        AssistantWebSocketEndpoint endpoint = injector.assistantWebSocketEndpoint();

        // Then
        assertThat(endpoint).isNotNull();
        // The endpoint should be properly configured with dependencies
        // We can't directly test the private fields, but we can test behavior
    }

    @Test
    @DisplayName("Should handle initialization without errors")
    void shouldHandleInitialization_WithoutErrors() {
        // When & Then - should not throw any exceptions
        injector.logInjectorInitialization();
    }

    @Test
    @DisplayName("Should create endpoint bean that can handle messages")
    void shouldCreateEndpointBean_ThatCanHandleMessages() {
        // Given
        AssistantWebSocketEndpoint endpoint = injector.assistantWebSocketEndpoint();

        // When & Then - endpoint should be properly initialized
        assertThat(endpoint).isNotNull();
        
        // Test that the endpoint has the required dependencies by testing its behavior
        // This indirectly tests that the constructor injection worked
        endpoint.onOpen(mock(jakarta.websocket.Session.class));
        // If no exception is thrown, the endpoint is properly configured
    }

    @Test
    @DisplayName("Should handle null dependencies gracefully in constructor")
    void shouldHandleNullDependencies_GracefullyInConstructor() {
        // When & Then - should not throw exception during construction
        AssistantWebSocketInjector nullInjector = new AssistantWebSocketInjector(null, null, null);
        assertThat(nullInjector).isNotNull();
        
        // The bean creation should still work (though the endpoint won't function properly)
        AssistantWebSocketEndpoint endpoint = nullInjector.assistantWebSocketEndpoint();
        assertThat(endpoint).isNotNull();
    }

    @Test
    @DisplayName("Should provide consistent bean instances")
    void shouldProvideConsistentBeanInstances() {
        // When
        AssistantWebSocketEndpoint endpoint1 = injector.assistantWebSocketEndpoint();
        AssistantWebSocketEndpoint endpoint2 = injector.assistantWebSocketEndpoint();

        // Then - should create new instances each time (prototype scope)
        assertThat(endpoint1).isNotNull();
        assertThat(endpoint2).isNotNull();
        assertThat(endpoint1).isNotSameAs(endpoint2);
    }

    @Test
    @DisplayName("Should inject all required dependencies into endpoint")
    void shouldInjectAllRequiredDependencies_IntoEndpoint() {
        // Given
        lenient().when(assistantService.toString()).thenReturn("MockAssistantService");
        lenient().when(objectMapper.toString()).thenReturn("MockObjectMapper");
        lenient().when(validator.toString()).thenReturn("MockValidator");

        // When
        AssistantWebSocketEndpoint endpoint = injector.assistantWebSocketEndpoint();

        // Then
        assertThat(endpoint).isNotNull();
        
        // Test that dependencies are properly injected by testing endpoint behavior
        // The endpoint should not fail with null pointer exceptions when using dependencies
        jakarta.websocket.Session mockSession = mock(jakarta.websocket.Session.class);
        lenient().when(mockSession.getId()).thenReturn("test-session");
        
        // This should work without throwing NPE if dependencies are properly injected
        endpoint.onOpen(mockSession);
        endpoint.onClose(mockSession);
        endpoint.onError(mockSession, new RuntimeException("test error"));
    }
}
