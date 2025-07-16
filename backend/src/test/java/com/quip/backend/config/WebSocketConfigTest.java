package com.quip.backend.config;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @DisplayName("Should instantiate WebSocketConfig successfully")
    void shouldInstantiateWebSocketConfig_Successfully() {
        // When & Then
        assertThat(webSocketConfig).isNotNull();
    }
}
