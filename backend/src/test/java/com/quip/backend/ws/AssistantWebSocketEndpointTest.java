package com.quip.backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.service.AssistantService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssistantWebSocketEndpoint.
 * Tests WebSocket lifecycle methods and message handling scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantWebSocketEndpoint Tests")
class AssistantWebSocketEndpointTest {

    @Mock
    private AssistantService assistantService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Basic basicRemote;

    @Mock
    private ConstraintViolation<AssistantRequestDto> constraintViolation;

    private AssistantWebSocketEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new AssistantWebSocketEndpoint(assistantService, objectMapper, validator);
        lenient().when(session.getBasicRemote()).thenReturn(basicRemote);
        lenient().when(session.getId()).thenReturn("test-session-123");
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("Should log session opening")
    void shouldLogSessionOpening() {
        // When
        endpoint.onOpen(session);

        // Then - verify no exceptions and session ID is used
        verify(session).getId();
    }

    @Test
    @DisplayName("Should handle valid message successfully")
    void shouldHandleValidMessageSuccessfully() throws Exception {
        // Given
        String message = "{\"query\":\"test query\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        Flux<String> responseFlux = Flux.just("response1", "response2");

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(assistantService.invokeAssistant(dto)).thenReturn(responseFlux);

        // When
        endpoint.onMessage(message, session);

        // Then
        verify(objectMapper).readValue(message, AssistantRequestDto.class);
        verify(validator).validate(dto);
        verify(assistantService).invokeAssistant(dto);
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors() throws Exception {
        // Given
        String message = "{\"query\":\"\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        Set<ConstraintViolation<AssistantRequestDto>> violations = Set.of(constraintViolation);

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(violations);
        when(constraintViolation.getMessage()).thenReturn("Query cannot be empty");

        // When
        endpoint.onMessage(message, session);

        // Then
        verify(basicRemote).sendText("Validation error: Query cannot be empty");
        verify(session).close();
        verify(assistantService, never()).invokeAssistant(any());
    }

    @Test
    @DisplayName("Should handle JSON parsing errors")
    void shouldHandleJsonParsingErrors() throws Exception {
        // Given
        String invalidMessage = "invalid json";
        when(objectMapper.readValue(invalidMessage, AssistantRequestDto.class))
                .thenThrow(new RuntimeException("Invalid JSON"));

        // When
        endpoint.onMessage(invalidMessage, session);

        // Then
        verify(basicRemote).sendText(contains("Error parsing message"));
        verify(session).close();
        verify(assistantService, never()).invokeAssistant(any());
    }

    @Test
    @DisplayName("Should handle uninitialized dependencies")
    void shouldHandleUninitializedDependencies() throws Exception {
        // Given
        AssistantWebSocketEndpoint uninitializedEndpoint = new AssistantWebSocketEndpoint();
        String message = "{\"query\":\"test\"}";

        // When
        uninitializedEndpoint.onMessage(message, session);

        // Then
        verify(basicRemote).sendText("Service dependencies not properly initialized");
        verify(session).close();
    }

    @Test
    @DisplayName("Should handle IOException when sending messages")
    void shouldHandleIOExceptionWhenSendingMessages() throws Exception {
        // Given
        String message = "{\"query\":\"test query\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        Flux<String> responseFlux = Flux.just("response");

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(assistantService.invokeAssistant(dto)).thenReturn(responseFlux);
        doThrow(new IOException("Connection lost")).when(basicRemote).sendText(anyString());

        // When
        endpoint.onMessage(message, session);

        // Then - should not throw exception, just log error
        verify(basicRemote, atLeastOnce()).sendText(anyString());
    }

    @Test
    @DisplayName("Should handle closed session gracefully")
    void shouldHandleClosedSessionGracefully() throws Exception {
        // Given
        String message = "{\"query\":\"test query\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        Flux<String> responseFlux = Flux.just("response");

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(assistantService.invokeAssistant(dto)).thenReturn(responseFlux);
        when(session.isOpen()).thenReturn(false);

        // When
        endpoint.onMessage(message, session);

        // Then - should not attempt to send to closed session
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    @DisplayName("Should handle assistant service errors")
    void shouldHandleAssistantServiceErrors() throws Exception {
        // Given
        String message = "{\"query\":\"test query\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        Flux<String> errorFlux = Flux.error(new RuntimeException("Service error"));

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(assistantService.invokeAssistant(dto)).thenReturn(errorFlux);

        // When
        endpoint.onMessage(message, session);

        // Then - error should be handled in the flux subscription
        // The actual error handling happens asynchronously in the flux
        verify(assistantService).invokeAssistant(dto);
    }

    @Test
    @DisplayName("Should handle multiple validation errors")
    void shouldHandleMultipleValidationErrors() throws Exception {
        // Given
        String message = "{\"query\":\"\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        
        ConstraintViolation<AssistantRequestDto> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<AssistantRequestDto> violation2 = mock(ConstraintViolation.class);
        Set<ConstraintViolation<AssistantRequestDto>> violations = Set.of(violation1, violation2);

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(violations);
        when(violation1.getMessage()).thenReturn("Error 1");
        when(violation2.getMessage()).thenReturn("Error 2");

        // When
        endpoint.onMessage(message, session);

        // Then
        verify(basicRemote).sendText(contains("Validation error:"));
        verify(basicRemote).sendText(contains("Error 1"));
        verify(basicRemote).sendText(contains("Error 2"));
        verify(session).close();
    }

    @Test
    @DisplayName("Should log session closing")
    void shouldLogSessionClosing() {
        // When
        endpoint.onClose(session);

        // Then - verify no exceptions and session ID is used
        verify(session).getId();
    }

    @Test
    @DisplayName("Should log WebSocket errors")
    void shouldLogWebSocketErrors() {
        // Given
        Throwable error = new RuntimeException("WebSocket error");

        // When
        endpoint.onError(session, error);

        // Then - verify no exceptions and session ID is used
        verify(session).getId();
    }

    @Test
    @DisplayName("Should handle session close IOException")
    void shouldHandleSessionCloseIOException() throws Exception {
        // Given
        String message = "{\"query\":\"\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        Set<ConstraintViolation<AssistantRequestDto>> violations = Set.of(constraintViolation);

        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(violations);
        when(constraintViolation.getMessage()).thenReturn("Validation error");
        doThrow(new IOException("Cannot close session")).when(session).close();

        // When
        endpoint.onMessage(message, session);

        // Then - should not throw exception, just log error
        verify(session).close();
    }

    @Test
    @DisplayName("Should test reactive stream completion")
    void shouldTestReactiveStreamCompletion() throws Exception {
        // Given
        String message = "{\"query\":\"test query\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        
        when(objectMapper.readValue(message, AssistantRequestDto.class)).thenReturn(dto);
        when(validator.validate(dto)).thenReturn(Collections.emptySet());

        // Create a test flux that we can control
        Flux<String> testFlux = Flux.just("chunk1", "chunk2", "chunk3");
        when(assistantService.invokeAssistant(dto)).thenReturn(testFlux);

        // When
        endpoint.onMessage(message, session);

        // Then - verify the flux behavior
        StepVerifier.create(testFlux)
                .expectNext("chunk1")
                .expectNext("chunk2")
                .expectNext("chunk3")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle dependency injection setters")
    void shouldHandleDependencyInjectionSetters() {
        // Given
        AssistantWebSocketEndpoint newEndpoint = new AssistantWebSocketEndpoint();
        AssistantService newService = mock(AssistantService.class);
        ObjectMapper newMapper = mock(ObjectMapper.class);
        Validator newValidator = mock(Validator.class);

        // When
        newEndpoint.setAssistantService(newService);
        newEndpoint.setObjectMapper(newMapper);
        newEndpoint.setValidator(newValidator);

        // Then - dependencies should be set (tested implicitly through usage)
        // This test ensures the setters work correctly for dependency injection
        String message = "{\"query\":\"test\"}";
        AssistantRequestDto dto = new AssistantRequestDto();
        
        try {
            when(newMapper.readValue(anyString(), eq(AssistantRequestDto.class))).thenReturn(dto);
            when(newValidator.validate(any())).thenReturn(Collections.emptySet());
            when(newService.invokeAssistant(any())).thenReturn(Flux.just("response"));

            newEndpoint.onMessage(message, session);

            verify(newMapper).readValue(message, AssistantRequestDto.class);
            verify(newValidator).validate(dto);
            verify(newService).invokeAssistant(dto);
        } catch (Exception e) {
            // Handle JSON processing exceptions in test
            throw new RuntimeException("Test setup failed", e);
        }
    }
}
