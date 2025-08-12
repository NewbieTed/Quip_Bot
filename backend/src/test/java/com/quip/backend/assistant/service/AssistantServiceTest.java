package com.quip.backend.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.dto.response.AgentResponseDto;
import com.quip.backend.assistant.model.database.AssistantConversation;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.model.Member;
import com.quip.backend.server.model.Server;
import com.quip.backend.tool.service.ToolWhitelistService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private ObjectMapper objectMapper;

    @Mock
    private ToolWhitelistService toolWhitelistService;

    @Mock
    private AssistantConversationService assistantConversationService;

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
        reset(authorizationService, webClient, objectMapper, toolWhitelistService, assistantConversationService, requestBodyUriSpec, requestBodySpec, requestHeadersSpec, responseSpec);

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
        void shouldCompleteFlux_WhenHttpStreamFinishesSuccessfully() throws Exception {
            // Given
            setupCommonMocksWithWebClient();

            String jsonResponse = "{\"content\":\"chunk-final\"}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto agentResponse = new AgentResponseDto();
            agentResponse.setContent("chunk-final");
            when(objectMapper.readValue(jsonResponse, AgentResponseDto.class)).thenReturn(agentResponse);

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
            setupCommonMocksWithWebClient();
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
        void shouldEmitChunksFromHttpStream_WhenRequestSuccessful() throws Exception {
            // Given
            setupCommonMocksWithWebClient();

            String jsonResponse1 = "{\"content\":\"chunk-1\"}";
            String jsonResponse2 = "{\"content\":\"chunk-2\"}";
            DataBuffer dataBuffer1 = new DefaultDataBufferFactory().wrap(jsonResponse1.getBytes(StandardCharsets.UTF_8));
            DataBuffer dataBuffer2 = new DefaultDataBufferFactory().wrap(jsonResponse2.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer1, dataBuffer2));

            AgentResponseDto agentResponse1 = new AgentResponseDto();
            agentResponse1.setContent("chunk-1");
            AgentResponseDto agentResponse2 = new AgentResponseDto();
            agentResponse2.setContent("chunk-2");
            
            when(objectMapper.readValue(jsonResponse1, AgentResponseDto.class)).thenReturn(agentResponse1);
            when(objectMapper.readValue(jsonResponse2, AgentResponseDto.class)).thenReturn(agentResponse2);

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
            setupCommonMocksWithWebClient();
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

            setupCommonMocksWithWebClient();
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
            setupCommonMocks();

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
            setupCommonMocksWithWebClient();
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
            setupCommonMocksWithWebClient();
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
                    "memberId", VALID_MEMBER_ID,
                    "assistantConversationId", 1L
            );
            verify(requestBodySpec).bodyValue(expectedPayload);
        }

        @Test
        @DisplayName("Should handle JSON parsing error and fallback to plain text")
        void shouldHandleJsonParsingError_AndFallbackToPlainText() throws Exception {
            // Given
            setupCommonMocksWithWebClient();

            String invalidJson = "plain text response";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(invalidJson.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            when(objectMapper.readValue(invalidJson, AgentResponseDto.class))
                    .thenThrow(new RuntimeException("Invalid JSON"));

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectNext("plain text response")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should filter out empty content responses")
        void shouldFilterOutEmptyContentResponses() throws Exception {
            // Given
            setupCommonMocksWithWebClient();

            String emptyContentJson = "{\"content\":\"\"}";
            String validContentJson = "{\"content\":\"valid content\"}";
            DataBuffer dataBuffer1 = new DefaultDataBufferFactory().wrap(emptyContentJson.getBytes(StandardCharsets.UTF_8));
            DataBuffer dataBuffer2 = new DefaultDataBufferFactory().wrap(validContentJson.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer1, dataBuffer2));

            AgentResponseDto emptyResponse = new AgentResponseDto();
            emptyResponse.setContent("");
            AgentResponseDto validResponse = new AgentResponseDto();
            validResponse.setContent("valid content");
            
            when(objectMapper.readValue(emptyContentJson, AgentResponseDto.class)).thenReturn(emptyResponse);
            when(objectMapper.readValue(validContentJson, AgentResponseDto.class)).thenReturn(validResponse);

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectNext("valid content")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle null content in JSON response")
        void shouldHandleNullContentInJsonResponse() throws Exception {
            // Given
            setupCommonMocksWithWebClient();

            String nullContentJson = "{\"content\":null}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(nullContentJson.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto nullContentResponse = new AgentResponseDto();
            nullContentResponse.setContent(null);
            when(objectMapper.readValue(nullContentJson, AgentResponseDto.class)).thenReturn(nullContentResponse);

            // When & Then - null content should be filtered out, so stream completes without emitting
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle agent response with tool metadata")
        void shouldHandleAgentResponseWithToolMetadata() throws Exception {
            // Given
            setupCommonMocksWithWebClient();

            String jsonWithMetadata = "{\"content\":\"Tool executed successfully\",\"tool_name\":\"test_tool\",\"type\":\"interrupt\"}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(jsonWithMetadata.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto responseWithMetadata = new AgentResponseDto();
            responseWithMetadata.setContent("Tool executed successfully");
            responseWithMetadata.setToolName("test_tool");
            responseWithMetadata.setType("interrupt");
            when(objectMapper.readValue(jsonWithMetadata, AgentResponseDto.class)).thenReturn(responseWithMetadata);

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectNext("Tool executed successfully")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should include approved=false in payload when conversation is interrupted")
        void shouldIncludeApprovedFalseInPayload_WhenConversationIsInterrupted() {
            // Given
            setupCommonMocks();
            
            // Mock interrupted conversation
            AssistantConversation interruptedConversation = AssistantConversation.builder()
                    .id(1L)
                    .isInterrupt(true)
                    .build();
            when(assistantConversationService.validateAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
                    .thenReturn(interruptedConversation);
            
            setupWebClientMocks();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            assistantService.invokeAssistant(validRequestDto).subscribe();

            // Then
            Map<String, Object> expectedPayload = new HashMap<>();
            expectedPayload.put("message", VALID_MESSAGE);
            expectedPayload.put("serverId", VALID_SERVER_ID);
            expectedPayload.put("channelId", VALID_CHANNEL_ID);
            expectedPayload.put("memberId", VALID_MEMBER_ID);
            expectedPayload.put("assistantConversationId", 1L);
            expectedPayload.put("approved", false);
            
            verify(requestBodySpec).bodyValue(expectedPayload);
        }

        @Test
        @DisplayName("Should not include approved field when conversation is not interrupted")
        void shouldNotIncludeApprovedField_WhenConversationIsNotInterrupted() {
            // Given
            setupCommonMocksWithWebClient();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            assistantService.invokeAssistant(validRequestDto).subscribe();

            // Then
            Map<String, Object> expectedPayload = Map.of(
                    "message", VALID_MESSAGE,
                    "serverId", VALID_SERVER_ID,
                    "channelId", VALID_CHANNEL_ID,
                    "memberId", VALID_MEMBER_ID,
                    "assistantConversationId", 1L
            );
            verify(requestBodySpec).bodyValue(expectedPayload);
        }
    }

    @Nested
    @DisplayName("invokeNewAssistant() Tests")
    class InvokeNewAssistantTests {

        @Test
        @DisplayName("Should complete Flux when HTTP stream finishes successfully")
        void shouldCompleteFlux_WhenHttpStreamFinishesSuccessfully() throws Exception {
            // Given
            setupCommonMocksForNewAssistant();

            String jsonResponse = "{\"content\":\"new-assistant-response\"}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto agentResponse = new AgentResponseDto();
            agentResponse.setContent("new-assistant-response");
            when(objectMapper.readValue(jsonResponse, AgentResponseDto.class)).thenReturn(agentResponse);

            // When & Then
            StepVerifier.create(assistantService.invokeNewAssistant(validRequestDto))
                    .expectNext("new-assistant-response")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should validate authorization before making HTTP request")
        void shouldValidateAuthorization_BeforeMakingHttpRequest() {
            // Given
            setupCommonMocksForNewAssistant();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            Flux<String> result = assistantService.invokeNewAssistant(validRequestDto);

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
        @DisplayName("Should include whitelisted tools in payload")
        void shouldIncludeWhitelistedToolsInPayload() {
            // Given
            List<String> whitelistedTools = Arrays.asList("tool1", "tool2", "tool3");
            setupCommonMocksForNewAssistant();
            when(toolWhitelistService.getWhitelistedToolNamesForNewConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
                    .thenReturn(whitelistedTools);
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            assistantService.invokeNewAssistant(validRequestDto).subscribe();

            // Then
            Map<String, Object> expectedPayload = Map.of(
                    "message", VALID_MESSAGE,
                    "serverId", VALID_SERVER_ID,
                    "channelId", VALID_CHANNEL_ID,
                    "memberId", VALID_MEMBER_ID,
                    "whitelistedTools", whitelistedTools
            );
            verify(requestBodySpec).bodyValue(expectedPayload);
        }

        @Test
        @DisplayName("Should handle empty whitelisted tools list")
        void shouldHandleEmptyWhitelistedToolsList() {
            // Given
            List<String> emptyWhitelistedTools = Collections.emptyList();
            setupCommonMocksForNewAssistant();
            when(toolWhitelistService.getWhitelistedToolNamesForNewConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
                    .thenReturn(emptyWhitelistedTools);
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());

            // When
            Flux<String> result = assistantService.invokeNewAssistant(validRequestDto);

            // Then
            assertNotNull(result);
            Map<String, Object> expectedPayload = Map.of(
                    "message", VALID_MESSAGE,
                    "serverId", VALID_SERVER_ID,
                    "channelId", VALID_CHANNEL_ID,
                    "memberId", VALID_MEMBER_ID,
                    "whitelistedTools", emptyWhitelistedTools
            );
            verify(requestBodySpec).bodyValue(expectedPayload);
        }

        @Test
        @DisplayName("Should emit chunks from HTTP stream when request is successful")
        void shouldEmitChunksFromHttpStream_WhenRequestSuccessful() throws Exception {
            // Given
            setupCommonMocksForNewAssistant();

            String jsonResponse1 = "{\"content\":\"new-chunk-1\"}";
            String jsonResponse2 = "{\"content\":\"new-chunk-2\"}";
            DataBuffer dataBuffer1 = new DefaultDataBufferFactory().wrap(jsonResponse1.getBytes(StandardCharsets.UTF_8));
            DataBuffer dataBuffer2 = new DefaultDataBufferFactory().wrap(jsonResponse2.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer1, dataBuffer2));

            AgentResponseDto agentResponse1 = new AgentResponseDto();
            agentResponse1.setContent("new-chunk-1");
            AgentResponseDto agentResponse2 = new AgentResponseDto();
            agentResponse2.setContent("new-chunk-2");
            
            when(objectMapper.readValue(jsonResponse1, AgentResponseDto.class)).thenReturn(agentResponse1);
            when(objectMapper.readValue(jsonResponse2, AgentResponseDto.class)).thenReturn(agentResponse2);

            // When
            Flux<String> result = assistantService.invokeNewAssistant(validRequestDto);

            // Then
            StepVerifier.create(result)
                    .expectNext("new-chunk-1")
                    .expectNext("new-chunk-2")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
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
            assertThatThrownBy(() -> assistantService.invokeNewAssistant(validRequestDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Authorization failed");
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
            
            when(toolWhitelistService.getWhitelistedToolNamesForNewConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
                    .thenReturn(Collections.emptyList());

            when(webClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.accept(MediaType.TEXT_PLAIN)).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("HTTP client error"));

            // When & Then
            assertThatThrownBy(() -> assistantService.invokeNewAssistant(validRequestDto).blockFirst())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("HTTP client error");
        }

        @Test
        @DisplayName("Should handle error from HTTP response stream")
        void shouldHandleErrorFromHttpResponseStream() {
            // Given
            setupCommonMocksForNewAssistant();
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.error(new RuntimeException("Stream error")));

            // When & Then
            StepVerifier.create(assistantService.invokeNewAssistant(validRequestDto))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("Stream error"))
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle JSON parsing error and fallback to plain text")
        void shouldHandleJsonParsingError_AndFallbackToPlainText() throws Exception {
            // Given
            setupCommonMocksForNewAssistant();

            String invalidJson = "plain text response from new assistant";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(invalidJson.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            when(objectMapper.readValue(invalidJson, AgentResponseDto.class))
                    .thenThrow(new RuntimeException("Invalid JSON"));

            // When & Then
            StepVerifier.create(assistantService.invokeNewAssistant(validRequestDto))
                    .expectNext("plain text response from new assistant")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should filter out empty content responses")
        void shouldFilterOutEmptyContentResponses() throws Exception {
            // Given
            setupCommonMocksForNewAssistant();

            String emptyContentJson = "{\"content\":\"\"}";
            String validContentJson = "{\"content\":\"valid new content\"}";
            DataBuffer dataBuffer1 = new DefaultDataBufferFactory().wrap(emptyContentJson.getBytes(StandardCharsets.UTF_8));
            DataBuffer dataBuffer2 = new DefaultDataBufferFactory().wrap(validContentJson.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer1, dataBuffer2));

            AgentResponseDto emptyResponse = new AgentResponseDto();
            emptyResponse.setContent("");
            AgentResponseDto validResponse = new AgentResponseDto();
            validResponse.setContent("valid new content");
            
            when(objectMapper.readValue(emptyContentJson, AgentResponseDto.class)).thenReturn(emptyResponse);
            when(objectMapper.readValue(validContentJson, AgentResponseDto.class)).thenReturn(validResponse);

            // When & Then
            StepVerifier.create(assistantService.invokeNewAssistant(validRequestDto))
                    .expectNext("valid new content")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle null content in JSON response")
        void shouldHandleNullContentInJsonResponse() throws Exception {
            // Given
            setupCommonMocksForNewAssistant();

            String nullContentJson = "{\"content\":null}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(nullContentJson.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto nullContentResponse = new AgentResponseDto();
            nullContentResponse.setContent(null);
            when(objectMapper.readValue(nullContentJson, AgentResponseDto.class)).thenReturn(nullContentResponse);

            // When & Then
            StepVerifier.create(assistantService.invokeNewAssistant(validRequestDto))
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle null request DTO")
        void shouldHandleNullRequestDto() {
            // When & Then
            assertThatThrownBy(() -> assistantService.invokeNewAssistant(null))
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
            assertThatThrownBy(() -> assistantService.invokeNewAssistant(requestWithNullMemberId))
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
            assertThatThrownBy(() -> assistantService.invokeNewAssistant(requestWithNullChannelId))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("extractContentFromAgentResponse() Tests")
    class ExtractContentFromAgentResponseTests {

        @Test
        @DisplayName("Should extract content from valid JSON response")
        void shouldExtractContentFromValidJsonResponse() throws Exception {
            // Given
            setupCommonMocksWithWebClient();
            
            String jsonResponse = "{\"content\":\"test content\",\"tool_name\":\"test_tool\",\"type\":\"message\"}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto agentResponse = new AgentResponseDto();
            agentResponse.setContent("test content");
            agentResponse.setToolName("test_tool");
            agentResponse.setType("message");
            when(objectMapper.readValue(jsonResponse, AgentResponseDto.class)).thenReturn(agentResponse);

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectNext("test content")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() throws Exception {
            // Given
            setupCommonMocksWithWebClient();
            
            DataBuffer nullDataBuffer = new DefaultDataBufferFactory().wrap("".getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(nullDataBuffer));

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should return empty string for empty input")
        void shouldReturnEmptyStringForEmptyInput() throws Exception {
            // Given
            setupCommonMocksWithWebClient();
            
            String emptyResponse = "   ";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(emptyResponse.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should handle whitespace-only content")
        void shouldHandleWhitespaceOnlyContent() throws Exception {
            // Given
            setupCommonMocksWithWebClient();
            
            String jsonWithWhitespaceContent = "{\"content\":\"   \"}";
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(jsonWithWhitespaceContent.getBytes(StandardCharsets.UTF_8));
            when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.just(dataBuffer));

            AgentResponseDto agentResponse = new AgentResponseDto();
            agentResponse.setContent("   ");
            when(objectMapper.readValue(jsonWithWhitespaceContent, AgentResponseDto.class)).thenReturn(agentResponse);

            // When & Then
            StepVerifier.create(assistantService.invokeAssistant(validRequestDto))
                    .expectComplete()
                    .verify(Duration.ofSeconds(1));
        }
    }

    private void setupCommonMocksForNewAssistant() {
        // Setup authorization service mock
        when(authorizationService.validateAuthorization(
                VALID_MEMBER_ID,
                VALID_CHANNEL_ID,
                AuthorizationConstants.INVOKE_ASSISTANT,
                INVOKE_ASSISTANT_OPERATION
        )).thenReturn(mockAuthorizationContext);

        when(toolWhitelistService.getWhitelistedToolNamesForNewConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
                .thenReturn(Arrays.asList("tool1", "tool2"));
        setupWebClientMocks();
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

    private void setupCommonMocks() {
        // Setup authorization service mock
        when(authorizationService.validateAuthorization(
                VALID_MEMBER_ID,
                VALID_CHANNEL_ID,
                AuthorizationConstants.INVOKE_ASSISTANT,
                INVOKE_ASSISTANT_OPERATION
        )).thenReturn(mockAuthorizationContext);

        // Setup assistant conversation service mock
        AssistantConversation mockConversation = AssistantConversation.builder()
                .id(1L)
                .isInterrupt(false)
                .build();
        when(assistantConversationService.validateAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
            .thenReturn(mockConversation);
    }

    private void setupCommonMocksWithWebClient() {
        setupCommonMocks();
        setupWebClientMocks();
    }
}
