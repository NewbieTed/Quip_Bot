package com.quip.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

/**
 * Configuration class for WebSocket functionality.
 * <p>
 * This class configures both WebSocket client and server components.
 * It provides beans for creating WebSocket connections to external services
 * and for exposing WebSocket endpoints within this application.
 * </p>
 */
@Configuration
public class WebSocketConfig {
    /**
     * Creates a WebSocket client for connecting to external WebSocket services.
     * <p>
     * This client is used by the AssistantService to establish WebSocket connections
     * to the AI assistant service.
     * </p>
     *
     * @return A reactive WebSocket client implementation
     */
    @Bean
    public ReactorNettyWebSocketClient webSocketClient() {
        return new ReactorNettyWebSocketClient();
    }

    /**
     * Creates a server endpoint exporter for exposing WebSocket endpoints.
     * <p>
     * This bean scans for classes annotated with @ServerEndpoint and registers
     * them as WebSocket endpoints, making them available for clients to connect to.
     * </p>
     *
     * @return A server endpoint exporter
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}