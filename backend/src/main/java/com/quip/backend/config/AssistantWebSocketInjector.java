package com.quip.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.ws.AssistantWebSocketEndpoint;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for injecting dependencies into WebSocket endpoints.
 * Since WebSocket endpoints are instantiated by the container, we need to manually inject dependencies.
 */
@Configuration
@RequiredArgsConstructor
public class AssistantWebSocketInjector {

    private final AssistantService assistantService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * Provides a configured AssistantWebSocketEndpoint bean for testing and manual instantiation.
     */
    @Bean
    public AssistantWebSocketEndpoint assistantWebSocketEndpoint() {
        return new AssistantWebSocketEndpoint(assistantService, objectMapper, validator);
    }

    /**
     * Note: The actual WebSocket endpoint instances are created by the WebSocket container,
     * so dependency injection happens through the AssistantWebSocketEndpoint setters
     * when the container creates instances.
     */
    @PostConstruct
    public void logInjectorInitialization() {
        // Log that the injector has been initialized
        // The actual injection happens when WebSocket instances are created
    }
}
