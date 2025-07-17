package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;

/**
 * Service responsible for AI assistant functionality.
 * <p>
 * This service handles interactions with the AI assistant system, providing
 * a bridge between the user interface and the AI backend. It manages WebSocket
 * connections to the AI service and handles authorization for assistant invocations.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {
    private final AuthorizationService authorizationService;
    private final ReactorNettyWebSocketClient webSocketClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INVOKE_ASSISTANT = "Invoke Assistant";

    /**
     * Invokes the AI assistant with the given request and streams back responses.
     * <p>
     * This method establishes a WebSocket connection to the AI backend service,
     * sends the user's request, and returns a reactive stream of responses from
     * the assistant. It first validates that the requesting member has proper
     * authorization to use the assistant in the specified channel.
     * </p>
     *
     * @param assistantRequestDto The request containing member ID, channel ID, and message
     * @return A Flux of string responses from the assistant
     * @throws ValidationException If the member lacks proper authorization
     */
    public Flux<String> invokeAssistant(AssistantRequestDto assistantRequestDto) {
        // Validate authorization
        Long memberId = assistantRequestDto.getMemberId();
        Long channelId = assistantRequestDto.getChannelId();

        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                memberId,
                channelId,
                AuthorizationConstants.INVOKE_ASSISTANT,
                INVOKE_ASSISTANT
        );

        Long serverId = authorizationContext.server().getId();

        // Create a reactive stream that will emit responses from the AI assistant
        return Flux.create(sink ->
            // Connect to the AI assistant WebSocket endpoint
            webSocketClient.execute(
                // Using Docker internal hostname to connect to the AI service
                URI.create("ws://host.docker.internal:5001/assistant"),
                session -> {
                    try {
                        // Prepare the payload with all necessary context for the AI assistant
                        String payload = objectMapper.writeValueAsString(
                            Map.of(
                                    "message", assistantRequestDto.getMessage(),
                                    "serverId", serverId,
                                    "channelId", channelId,
                                    "memberId", memberId
                            )
                        );
                        
                        // Send the initial message to the WebSocket
                        return session.send(Mono.just(session.textMessage(payload)))
                            // Process incoming messages from the WebSocket
                            .thenMany(session.receive()
                                // Convert binary messages to text
                                .map(WebSocketMessage::getPayloadAsText)
                                // For each message received, emit it to our Flux
                                .doOnNext(message -> {
                                    log.info("Received chunk: {}", message);
                                    sink.next(message);
                                })
                                // Handle any errors during message reception
                                .doOnError(e -> {
                                    log.error("WebSocket receive error: {}", e.getMessage(), e);
                                    sink.error(e);
                                })
                                .then())
                            // Handle WebSocket session completion
                            .doFinally(signal -> {
                                log.info("WebSocket session closed with signal: {}", signal);
                                sink.complete();
                            })
                            .then();
                    } catch (Exception e) {
                        // Handle any errors during WebSocket setup or JSON serialization
                        log.error("WebSocket initialization error: {}", e.getMessage(), e);
                        sink.error(e);
                        return Mono.empty();
                    }
                }
            // Subscribe to activate the WebSocket connection
            ).subscribe()
        );
    }
}
