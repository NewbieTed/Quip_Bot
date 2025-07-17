package com.quip.backend.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.model.Member;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.server.model.Server;
import com.quip.backend.server.service.ServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
 * authorization validation and WebSocket connection handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantService Tests")
public class AssistantServiceTest extends BaseTest {

    @InjectMocks
    private AssistantService assistantService;

    @Mock
    private MemberService memberService;

    @Mock
    private ChannelService channelService;

    @Mock
    private ServerService serverService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ReactorNettyWebSocketClient webSocketClient;

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
        reset(memberService, channelService, serverService, authorizationService, webSocketClient);

        setupValidRequestDto();
        setupMockAuthorizationContext();
    }

    @Nested
    @DisplayName("invokeAssistant() Tests")
    class InvokeAssistantTests {
        @Test
        @DisplayName("Should complete Flux when WebSocket session finishes successfully")
        void shouldCompleteFlux_WhenWebSocketSessionFinishesSuccessfully() {
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            WebSocketSession mockSession = mock(WebSocketSession.class);
            WebSocketMessage mockMessage = mock(WebSocketMessage.class);

            when(mockMessage.getPayloadAsText()).thenReturn("chunk-final");
            when(mockSession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
            when(mockSession.send(any())).thenReturn(Mono.empty());
            when(mockSession.receive()).thenReturn(Flux.just(mockMessage));

            when(webSocketClient.execute(any(), any())).thenAnswer(invocation -> {
                WebSocketHandler handler = invocation.getArgument(1);
                return handler.handle(mockSession);
            });

            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectNext("chunk-final")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should validate authorization before creating WebSocket connection")
        void shouldValidateAuthorization_BeforeCreatingWebSocketConnection() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

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
        @DisplayName("Should emit chunks from WebSocket when connection is successful")
        void shouldEmitChunksFromWebSocket_WhenConnectionSuccessful() {
            // Given
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            WebSocketSession mockSession = mock(WebSocketSession.class);
            WebSocketMessage mockMessage = mock(WebSocketMessage.class);

            when(mockMessage.getPayloadAsText()).thenReturn("chunk-1");
            when(mockSession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
            when(mockSession.send(any())).thenReturn(Mono.empty());
            when(mockSession.receive()).thenReturn(Flux.just(mockMessage));

            when(webSocketClient.execute(any(), any())).thenAnswer(invocation -> {
                WebSocketHandler handler = invocation.getArgument(1);
                return handler.handle(mockSession);
            });

            // When
            Flux<String> result = assistantService.invokeAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectNext("chunk-1")
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

            // When
            Flux<String> result = assistantService.invokeAssistant(validRequestDto);

            // Then
            assertNotNull(result);
            
            // Note: We can't easily test WebSocket connections in unit tests,
            // but we can verify that the Flux is created and authorization is called
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

            // When
            Flux<String> result = assistantService.invokeAssistant(requestWithNullMessage);

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
        @DisplayName("Should handle exception during WebSocket session setup")
        void shouldHandleExceptionDuringWebSocketSetup() {
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            WebSocketSession mockSession = mock(WebSocketSession.class);
            when(mockSession.textMessage(anyString())).thenThrow(new RuntimeException("textMessage error"));

            when(webSocketClient.execute(any(), any())).thenAnswer(invocation -> {
                WebSocketHandler handler = invocation.getArgument(1);
                return handler.handle(mockSession);
            });

            // First: Simulate exception thrown in WebSocket session setup
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("textMessage error"))
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle JSON serialization exception and return empty Mono")
        void shouldHandleJsonSerializationException_AndReturnEmptyMono() {
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            // Create a request DTO that will cause JSON serialization to fail
            AssistantRequestDto problematicRequestDto = new AssistantRequestDto() {
                @Override
                public String getMessage() {
                    // This will cause ObjectMapper.writeValueAsString to throw an exception
                    throw new RuntimeException("JSON serialization failed");
                }
            };
            problematicRequestDto.setMemberId(VALID_MEMBER_ID);
            problematicRequestDto.setChannelId(VALID_CHANNEL_ID);

            when(webSocketClient.execute(any(), any())).thenAnswer(invocation -> {
                WebSocketHandler handler = invocation.getArgument(1);
                WebSocketSession mockSession = mock(WebSocketSession.class);
                return handler.handle(mockSession);
            });

            // The exception should be caught and sink.error should be called, 
            // then Mono.empty() should be returned from the catch block
            StepVerifier.create(assistantService.invokeAssistant(problematicRequestDto))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("JSON serialization failed"))
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should propagate error from WebSocket receive stream")
        void shouldPropagateErrorFromWebSocketReceive() {
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            WebSocketSession mockSession = mock(WebSocketSession.class);
            when(mockSession.textMessage(anyString())).thenReturn(mock(WebSocketMessage.class));
            when(mockSession.send(any())).thenReturn(Mono.empty());
            when(mockSession.receive()).thenReturn(Flux.error(new RuntimeException("receive failure")));

            when(webSocketClient.execute(any(), any())).thenAnswer(invocation -> {
                WebSocketHandler handler = invocation.getArgument(1);
                return handler.handle(mockSession);
            });

            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("receive failure"))
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should return proper Mono when exception occurs during WebSocket setup")
        void shouldReturnProperMono_WhenExceptionOccursDuringWebSocketSetup() {
            when(authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    AuthorizationConstants.INVOKE_ASSISTANT,
                    INVOKE_ASSISTANT_OPERATION
            )).thenReturn(mockAuthorizationContext);

            // Create a custom WebSocketClient that will test the catch block return value
            when(webSocketClient.execute(any(), any())).thenAnswer(invocation -> {
                WebSocketHandler handler = invocation.getArgument(1);
                WebSocketSession mockSession = mock(WebSocketSession.class);

                // This will cause an exception in the try block, leading to the catch block
                when(mockSession.textMessage(anyString())).thenThrow(new RuntimeException("WebSocket setup failed"));

                // Call the handler and capture the result
                Mono<Void> result = handler.handle(mockSession);

                // Verify that the result is not null (this will kill the surviving mutation)
                assertNotNull(result, "Handler should return a non-null Mono even when exception occurs");

                // Subscribe to the result to ensure it completes properly
                // This will exercise the Mono.empty() return path in the catch block
                result.subscribe(
                    value -> {}, // onNext - should not be called for Mono<Void>
                    error -> {}, // onError - should be called with our exception
                    () -> {}     // onComplete - should not be called due to error
                );

                return result;
            });

            // The Flux should handle the exception properly
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("WebSocket setup failed"))
                    .verify(Duration.ofSeconds(1));
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
