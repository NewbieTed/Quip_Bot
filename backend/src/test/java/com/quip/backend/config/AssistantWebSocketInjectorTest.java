package com.quip.backend.config;

import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.ws.AssistantWebSocketEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AssistantWebSocketInjector}.
 * <p>
 * This test class validates the WebSocket injector functionality including
 * proper service injection into the WebSocket endpoint.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantWebSocketInjector Tests")
class AssistantWebSocketInjectorTest extends BaseTest {

    @Mock
    private AssistantService assistantService;

    private AssistantWebSocketInjector injector;

    @BeforeEach
    void setUp() {
        injector = new AssistantWebSocketInjector(assistantService);
    }

    @Test
    @DisplayName("Should instantiate injector successfully")
    void shouldInstantiateInjector_Successfully() {
        // When & Then
        assertThat(injector).isNotNull();
    }

    @Test
    @DisplayName("Should inject AssistantService into WebSocket endpoint on init")
    void shouldInjectAssistantService_OnInit() {
        // Given
        try (MockedStatic<AssistantWebSocketEndpoint> mockedEndpoint = mockStatic(AssistantWebSocketEndpoint.class)) {
            
            // When
            injector.init();

            // Then
            mockedEndpoint.verify(() -> AssistantWebSocketEndpoint.setAssistantService(assistantService));
        }
    }

    @Test
    @DisplayName("Should handle null AssistantService gracefully")
    void shouldHandleNullAssistantService_Gracefully() {
        // Given
        AssistantWebSocketInjector nullInjector = new AssistantWebSocketInjector(null);
        
        try (MockedStatic<AssistantWebSocketEndpoint> mockedEndpoint = mockStatic(AssistantWebSocketEndpoint.class)) {
            
            // When
            nullInjector.init();

            // Then
            mockedEndpoint.verify(() -> AssistantWebSocketEndpoint.setAssistantService(null));
        }
    }
}
