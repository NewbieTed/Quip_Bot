package com.quip.backend.config;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WebSocketConfig}.
 * <p>
 * This test class validates the WebSocket configuration functionality including
 * proper bean creation and configuration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketConfig Tests")
class WebSocketConfigTest extends BaseTest {

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig();
    }

    @Test
    @DisplayName("Should create ServerEndpointExporter bean successfully")
    void shouldCreateServerEndpointExporter_Successfully() {
        // When
        ServerEndpointExporter result = webSocketConfig.serverEndpointExporter();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ServerEndpointExporter.class);
    }

    @Test
    @DisplayName("Should create new instance of ServerEndpointExporter on each call")
    void shouldCreateNewInstance_OnEachCall() {
        // When
        ServerEndpointExporter first = webSocketConfig.serverEndpointExporter();
        ServerEndpointExporter second = webSocketConfig.serverEndpointExporter();

        // Then
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("Should create ReactorNettyWebSocketClient bean successfully")
    void shouldCreateWebSocketClient_Successfully() {
        // When
        ReactorNettyWebSocketClient result = webSocketConfig.webSocketClient();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ReactorNettyWebSocketClient.class);
    }

    @Test
    @DisplayName("Should create new instance of ReactorNettyWebSocketClient on each call")
    void shouldCreateNewWebSocketClientInstance_OnEachCall() {
        // When
        ReactorNettyWebSocketClient first = webSocketConfig.webSocketClient();
        ReactorNettyWebSocketClient second = webSocketConfig.webSocketClient();

        // Then
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("Should instantiate WebSocketConfig successfully")
    void shouldInstantiateWebSocketConfig_Successfully() {
        // When & Then
        assertThat(webSocketConfig).isNotNull();
    }

    @Test
    @DisplayName("Should create both beans independently")
    void shouldCreateBothBeansIndependently_Successfully() {
        // When
        ServerEndpointExporter serverEndpointExporter = webSocketConfig.serverEndpointExporter();
        ReactorNettyWebSocketClient webSocketClient = webSocketConfig.webSocketClient();

        // Then
        assertThat(serverEndpointExporter).isNotNull();
        assertThat(webSocketClient).isNotNull();
        assertThat(serverEndpointExporter).isInstanceOf(ServerEndpointExporter.class);
        assertThat(webSocketClient).isInstanceOf(ReactorNettyWebSocketClient.class);
    }

    @Test
    @DisplayName("Should maintain bean independence across multiple calls")
    void shouldMaintainBeanIndependence_AcrossMultipleCalls() {
        // When
        ServerEndpointExporter firstExporter = webSocketConfig.serverEndpointExporter();
        ReactorNettyWebSocketClient firstClient = webSocketConfig.webSocketClient();
        ServerEndpointExporter secondExporter = webSocketConfig.serverEndpointExporter();
        ReactorNettyWebSocketClient secondClient = webSocketConfig.webSocketClient();

        // Then
        assertThat(firstExporter).isNotSameAs(secondExporter);
        assertThat(firstClient).isNotSameAs(secondClient);
        assertThat(firstExporter).isNotNull();
        assertThat(firstClient).isNotNull();
        assertThat(secondExporter).isNotNull();
        assertThat(secondClient).isNotNull();
    }
}
