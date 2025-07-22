package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.model.Member;
import com.quip.backend.server.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AssistantService}.
 * <p>
 * This test class validates the assistant service functionality including
 * authorization validation and HTTP streaming connection handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantService Tests")
public class AssistantServiceTest extends BaseTest {

    @InjectMocks
    private AssistantService assistantService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    // Test data constants
    private static final Long VALID_MEMBER_ID = 1L;
    private static final Long VALID_CHANNEL_ID = 10L;
    private static final Long VALID_SERVER_ID = 100L;
    private static final String VALID_MESSAGE = "Hello, Assistant!";
    private static final String INVOKE_ASSISTANT_OPERATION = "Invoke Assistant";

    private AssistantRequestDto validRequestDto;
    private AuthorizationContext mockAuthorizationContext;

    @BeforeEach
    void setUp() {
        reset(authorizationService, webClient, requestBodyUriSpec, requestBodySpec, requestHeadersSpec, responseSpec);

        setupValidRequestDto();
        setupMockAuthorizationContext();
    }

    private void setupWebClientMocks() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.TEXT_PLAIN)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Nested
    @DisplayName("invokeAssistant() Tests")
    class InvokeAssistantTests {
        @Test
        @DisplayName("Should complete Flux when HTTP stream finishes successfully")
        void shouldCompleteFlux_WhenHttpStreamFinishesSuccessfully() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();

            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap("chunk-final".getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectNext("chunk-final")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should validate authorization before making HTTP request")
        void shouldValidateAuthorization_BeforeMakingHttpRequest() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            Flux<String> result = assistantService.invokeAssistant(validRequestDto);

            // Then
            assertNotNull(result);
            verify(authorizationService).validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            );
        }

        @Test
        @DisplayName("Should emit chunks from HTTP stream when request is successful")
        void shouldEmitChunksFromHttpStream_WhenRequestSuccessful() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();

            DataBuffer dataBuffer1 = new DefaultDataBufferFactory().wrap("chunk-1".getBytes(StandardCharsets.UTF_8));
            DataBuffer dataBuffer2 = new DefaultDataBufferFactory().wrap("chunk-2".getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer1, dataBuffer2));

            // When
            Flux<String> result = assistantService.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectNext("chunk-1")
                    .expectNext("chunk-2")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));

            verify(authorizationService).validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            );
        }

        @Test
        @DisplayName("Should handle null request DTO")
        void shouldHandleNullRequestDto() {
            // When & Then
            assertThatThrownBy(() -> assistantService.invokeAssistant(null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(authorizationService);
        }

        @Test
        @DisplayName("Should handle request with null member ID")
        void shouldHandleRequestWithNullMemberId() {
            // Given
            AssistantRequestDto requestWithNullMemberId = new AssistantRequestDto();
            requestWithNullMemberId.setMemberId(null);
            requestWithNullMemberId.setChannelId(VALID_CHANNEL_ID);
            requestWithNullMemberId.setMessage(VALID_MESSAGE);

            // When & Then
            assertThatThrownBy(() -> assistantService.invokeAssistant(requestWithNullMemberId))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle request with null channel ID")
        void shouldHandleRequestWithNullChannelId() {
            // Given
            AssistantRequestDto requestWithNullChannelId = new AssistantRequestDto();
            requestWithNullChannelId.setMemberId(VALID_MEMBER_ID);
            requestWithNullChannelId.setChannelId(null);
            requestWithNullChannelId.setMessage(VALID_MESSAGE);

            // When & Then
            assertThatThrownBy(() -> assistantService.invokeAssistant(requestWithNullChannelId))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle authorization validation failure")
        void shouldHandleAuthorizationValidationFailure() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenThrow(new ValidationException("Authorization failed"));

            // When & Then
            assertThatThrownBy(() -> assistantService.invokeAssistant(validRequestDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Authorization failed");

            verify(authorizationService).validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            );
        }

        @Test
        @DisplayName("Should create Flux when authorization is successful")
        void shouldCreateFlux_WhenAuthorizationIsSuccessful() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            Flux<String> result = assistantService.invokeAssistant(validRequestDto);

            // Then
            assertNotNull(result);
            verify(authorizationService).validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            );
        }

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() {
            // Given
            AssistantRequestDto requestWithEmptyMessage = new AssistantRequestDto();
            requestWithEmptyMessage.setMemberId(VALID_MEMBER_ID);
            requestWithEmptyMessage.setChannelId(VALID_CHANNEL_ID);
            requestWithEmptyMessage.setMessage("");

            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            Flux<String> result = assistantService.invokeAssistant(requestWithEmptyMessage);

            // Then
            assertNotNull(result);
            verify(authorizationService).validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            );
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() {
            // Given
            AssistantRequestDto requestWithNullMessage = new AssistantRequestDto();
            requestWithNullMessage.setMemberId(VALID_MEMBER_ID);
            requestWithNullMessage.setChannelId(VALID_CHANNEL_ID);
            requestWithNullMessage.setMessage(null);

            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            // Expect a NullPointerException due to Map.of() not accepting null values
            assertThatThrownBy(() -> assistantService.invokeAssistant(requestWithNullMessage))
                    .isInstanceOf(NullPointerException.class);

            verify(authorizationService).validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            );
        }

        @Test
        @DisplayName("Should handle HTTP client error during request")
        void shouldHandleHttpClientError_DuringRequest() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.accept(MediaType.TEXT_PLAIN)).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("HTTP client error"));

            // When & Then - We expect the error to be propagated
            assertThatThrownBy(() -> assistantService.invokeAssistant(validRequestDto).blockFirst())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("HTTP client error");
        }

        @Test
        @DisplayName("Should handle error from HTTP response stream")
        void shouldHandleErrorFromHttpResponseStream() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.error(new RuntimeException("Stream error")));

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("Stream error"))
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should verify HTTP request payload contains correct data")
        void shouldVerifyHttpRequestPayload_ContainsCorrectData() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            setupWebClientMocks();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            assistantService.invokeAssistant(validRequestDto).subscribe();

            // Then
            verify(requestBodyUriSpec).uri("http://quip-agent:5000/assistant");
            verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
            verify(requestHeadersSpec).accept(MediaType.TEXT_PLAIN);
            
            // Verify the payload contains the expected data
            Map<String, Object> expectedPayload = Map.of(
                    "message", VALID_MESSAGE,
                    "serverId", VALID_SERVER_ID,
                    "channelId", VALID_CHANNEL_ID,
                    "memberId", VALID_MEMBER_ID
            );
            verify(requestBodySpec).bodyValue(expectedPayload);
        }
    }

    private void setupValidRequestDto() {
        validRequestDto = new AssistantRequestDto();
        validRequestDto.setMemberId(VALID_MEMBER_ID);
        validRequestDto.setChannelId(VALID_CHANNEL_ID);
        validRequestDto.setMessage(VALID_MESSAGE);
    }

    private void setupMockAuthorizationContext() {
        Member mockMember = new Member();
        mockMember.setId(VALID_MEMBER_ID);

        Channel mockChannel = new Channel();
        mockChannel.setId(VALID_CHANNEL_ID);
        mockChannel.setServerId(VALID_SERVER_ID);

        Server mockServer = new Server();
        mockServer.setId(VALID_SERVER_ID);

        mockAuthorizationContext = new AuthorizationContext(mockMember, mockChannel, mockServer, null);
    }
}
