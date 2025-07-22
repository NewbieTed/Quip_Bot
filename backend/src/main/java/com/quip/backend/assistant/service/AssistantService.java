package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Service responsible for AI assistant functionality.
 * <p>
 * This service handles interactions with the AI assistant system, providing
 * a bridge between the user interface and the AI backend. It manages HTTP
 * requests to the AI service and handles authorization for assistant invocations.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {
    private final AuthorizationService authorizationService;
    private final WebClient webClient;

    private static final String INVOKE_ASSISTANT = "Invoke Assistant";
    private static final String AGENT_BASE_URL = "http://quip-agent:5000";

    /**
     * Invokes the AI assistant with the given request and streams back responses.
     * <p>
     * This method sends an HTTP POST request to the AI agent service and returns
     * a reactive stream of responses from the assistant. It first validates that
     * the requesting member has proper authorization to use the assistant in the
     * specified channel.
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

        // Prepare the payload with all necessary context for the AI assistant
        Map<String, Object> payload = Map.of(
                "message", assistantRequestDto.getMessage(),
                "serverId", serverId,
                "channelId", channelId,
                "memberId", memberId
        );

        log.info("Sending request to agent with payload: {}", payload);

        // Make HTTP POST request to the agent and stream the response
        return webClient
                .post()
                .uri(AGENT_BASE_URL + "/assistant")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(dataBuffer -> {
                    // Convert DataBuffer to String to preserve exact formatting
                    String chunk = dataBuffer.toString(StandardCharsets.UTF_8);
                    DataBufferUtils.release(dataBuffer);
                    return chunk;
                })
                .doOnNext(chunk -> log.info("Received chunk from agent: [{}]", chunk.replace("\n", "\\n").replace("\r", "\\r")))
                .doOnError(error -> log.error("Error communicating with agent: {}", error.getMessage(), error))
                .doOnComplete(() -> log.info("Agent response stream completed"));
    }
}
