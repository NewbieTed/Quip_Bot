package com.quip.backend.assistant.controller;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AssistantController}.
 * <p>
 * This test class validates the assistant controller functionality including
 * HTTP request handling and streaming response behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantController Tests")
class AssistantControllerTest extends BaseTest {

    @InjectMocks
    private AssistantController assistantController;

    @Mock
    private AssistantService assistantService;

    // Test data constants
    private static final Long VALID_MEMBER_ID = 1L;
    private static final Long VALID_CHANNEL_ID = 10L;
    private static final String VALID_MESSAGE = "Hello, Assistant!";

    private AssistantRequestDto validRequestDto;

    @BeforeEach
    void setUp() {
        reset(assistantService);
        setupValidRequestDto();
    }

    @Test
    @DisplayName("Should instantiate controller successfully")
    void shouldInstantiateController_Successfully() {
        // When & Then
        assertThat(assistantController).isNotNull();
        assertThat(assistantService).isNotNull();
    }

    /**
     * Tests for the invokeAssistant endpoint which streams responses from the AI assistant.
     * This nested class validates that the endpoint correctly processes requests
     * and returns appropriate streaming responses.
     */
    @Nested
    @DisplayName("invokeAssistant Tests")
    class InvokeAssistantTests {

        @Test
        @DisplayName("Should return streaming response when service returns data")
        void shouldReturnStreamingResponse_WhenServiceReturnsData() {
            // Given
            Flux<String> mockResponse = Flux.just("chunk1", "chunk2", "chunk3");
            when(assistantService.invokeAssistant(any(AssistantRequestDto.class))).thenReturn(mockResponse);

            // When
            Flux<String> result = assistantController.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectNext("chunk1")
                    .expectNext("chunk2")
                    .expectNext("chunk3")
                    .verifyComplete();

            verify(assistantService).invokeAssistant(validRequestDto);
        }

        @Test
        @DisplayName("Should return empty flux when service returns empty flux")
        void shouldReturnEmptyFlux_WhenServiceReturnsEmptyFlux() {
            // Given
            when(assistantService.invokeAssistant(any(AssistantRequestDto.class))).thenReturn(Flux.empty());

            // When
            Flux<String> result = assistantController.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(assistantService).invokeAssistant(validRequestDto);
        }

        @Test
        @DisplayName("Should propagate error when service throws exception")
        void shouldPropagateError_WhenServiceThrowsException() {
            // Given
            RuntimeException testException = new RuntimeException("Service error");
            when(assistantService.invokeAssistant(any(AssistantRequestDto.class)))
                    .thenReturn(Flux.error(testException));

            // When
            Flux<String> result = assistantController.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(error -> error instanceof RuntimeException 
                            && "Service error".equals(error.getMessage()))
                    .verify();

            verify(assistantService).invokeAssistant(validRequestDto);
        }

        @Test
        @DisplayName("Should handle streaming response with multiple chunks")
        void shouldHandleStreamingResponse_WithMultipleChunks() {
            // Given
            Flux<String> mockResponse = Flux.just(
                    "Starting analysis...\n",
                    "Processing data...\n",
                    "Generating response...\n",
                    "Complete!"
            );
            when(assistantService.invokeAssistant(any(AssistantRequestDto.class))).thenReturn(mockResponse);

            // When
            Flux<String> result = assistantController.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectNext("Starting analysis...\n")
                    .expectNext("Processing data...\n")
                    .expectNext("Generating response...\n")
                    .expectNext("Complete!")
                    .verifyComplete();

            verify(assistantService).invokeAssistant(validRequestDto);
        }

        @Test
        @DisplayName("Should accept valid request with zero IDs")
        void shouldAcceptValidRequest_WithZeroIds() {
            // Given - Valid request with zero IDs (which are allowed by @PositiveOrZero)
            AssistantRequestDto validRequestWithZeros = new AssistantRequestDto();
            validRequestWithZeros.setMemberId(0L);
            validRequestWithZeros.setChannelId(0L);
            validRequestWithZeros.setMessage(VALID_MESSAGE);

            Flux<String> mockResponse = Flux.just("response");
            when(assistantService.invokeAssistant(any(AssistantRequestDto.class))).thenReturn(mockResponse);

            // When
            Flux<String> result = assistantController.invokeAssistant(validRequestWithZeros);

            // Then
            StepVerifier.create(result)
                    .expectNext("response")
                    .verifyComplete();

            verify(assistantService).invokeAssistant(validRequestWithZeros);
        }

        @Test
        @DisplayName("Should handle delayed streaming response")
        void shouldHandleDelayedStreamingResponse() {
            // Given
            Flux<String> mockResponse = Flux.just("chunk1", "chunk2", "chunk3").delayElements(java.time.Duration.ofMillis(10));
            when(assistantService.invokeAssistant(any(AssistantRequestDto.class))).thenReturn(mockResponse);

            // When
            Flux<String> result = assistantController.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectNext("chunk1")
                    .expectNext("chunk2")
                    .expectNext("chunk3")
                    .verifyComplete();

            verify(assistantService).invokeAssistant(validRequestDto);
        }
    }

    /**
     * Helper methods for test data setup
     */
    private void setupValidRequestDto() {
        validRequestDto = new AssistantRequestDto();
        validRequestDto.setMemberId(VALID_MEMBER_ID);
        validRequestDto.setChannelId(VALID_CHANNEL_ID);
        validRequestDto.setMessage(VALID_MESSAGE);
    }
}