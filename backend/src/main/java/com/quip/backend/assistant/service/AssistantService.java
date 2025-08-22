package com.quip.backend.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.dto.response.AgentResponseDto;
import com.quip.backend.assistant.dto.response.AgentToolDto;
import com.quip.backend.assistant.model.database.AssistantConversation;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.model.ToolWhitelist;
import com.quip.backend.tool.service.ToolWhitelistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ObjectMapper objectMapper;
    private final ToolWhitelistService toolWhitelistService;
    private final AssistantConversationService assistantConversationService;

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

        // Grab current active conversation
        AssistantConversation assistantConversation = assistantConversationService.validateAssistantConversation(memberId, serverId);

        // Check for concurrent processing protection
        if (!assistantConversationService.trySetProcessing(memberId, serverId, memberId)) {
            return Flux.error(new ValidationException("Another request is already being processed for this conversation"));
        }

        // Prepare the payload with all necessary context for the AI assistant
        Map<String, Object> payload = new HashMap<>(Map.of(
                "message", assistantRequestDto.getMessage(),
                "serverId", serverId,
                "channelId", channelId,
                "memberId", memberId,
                "assistantConversationId", assistantConversation.getId()
        ));

        // If message is sent while being interrupted, treat as rejecting operation
        if (assistantConversation.getIsInterrupt()) {
            payload.put("approved", false);
            // Clear interrupt status when user responds
            assistantConversationService.clearInterruptStatus(memberId, serverId, memberId);
        }

        log.info("Sending request to agent with payload: {}", payload);
        // TODO: Add update to interrupted state if it is indeed interrupted, this can be done via a API call from the agent to the backend
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
                .doOnNext(chunk -> log.info("Received raw chunk from agent: [{}]", chunk.replace("\n", "\\n").replace("\r", "\\r")))
                .map(this::extractContentFromAgentResponse)
                .filter(content -> !content.isEmpty())
                .doOnNext(content -> log.info("Extracted content: [{}]", content.replace("\n", "\\n").replace("\r", "\\r")))
                .doOnError(error -> {
                    log.error("Error communicating with agent: {}", error.getMessage(), error);
                    // Clear processing status on error
                    assistantConversationService.updateProcessingStatus(memberId, serverId, false, memberId);
                })
                .doOnComplete(() -> {
                    log.info("Agent response stream completed");
                    // Clear processing status on completion
                    assistantConversationService.updateProcessingStatus(memberId, serverId, false, memberId);
                })
                .doFinally(signalType -> {
                    // Ensure processing status is cleared regardless of how the stream ends
                    log.debug("Stream ended with signal: {}, ensuring processing status is cleared", signalType);
                    assistantConversationService.updateProcessingStatus(memberId, serverId, false, memberId);
                });
    }




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
    public Flux<String> invokeNewAssistant(AssistantRequestDto assistantRequestDto) {
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

        // For new conversations, we create a new conversation and set it as processing
        AssistantConversation newConversation = assistantConversationService.createActiveConversation(memberId, serverId, memberId);
        
        // Set processing status for the new conversation
        if (!assistantConversationService.updateProcessingStatus(memberId, serverId, true, memberId)) {
            log.warn("Failed to set processing status for new conversation");
        }

        // Get whitelisted tools for this member and server context
        List<String> whitelistedToolNames = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);

        // Prepare the payload with all necessary context for the AI assistant
        Map<String, Object> payload = Map.of(
                "message", assistantRequestDto.getMessage(),
                "serverId", serverId,
                "channelId", channelId,
                "memberId", memberId,
                "whitelistedTools", whitelistedToolNames
        );

        log.info("Sending request to new agent with payload: {}", payload);

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
                .doOnNext(chunk -> log.info("Received raw chunk from new agent: [{}]", chunk.replace("\n", "\\n").replace("\r", "\\r")))
                .map(this::extractContentFromAgentResponse)
                .filter(content -> !content.isEmpty())
                .doOnNext(content -> log.info("Extracted content from new agent: [{}]", content.replace("\n", "\\n").replace("\r", "\\r")))
                .doOnError(error -> {
                    log.error("Error communicating with agent: {}", error.getMessage(), error);
                    // Clear processing status on error
                    assistantConversationService.updateProcessingStatus(memberId, serverId, false, memberId);
                })
                .doOnComplete(() -> {
                    log.info("New agent response stream completed");
                    // Clear processing status on completion
                    assistantConversationService.updateProcessingStatus(memberId, serverId, false, memberId);
                })
                .doFinally(signalType -> {
                    // Ensure processing status is cleared regardless of how the stream ends
                    log.debug("New conversation stream ended with signal: {}, ensuring processing status is cleared", signalType);
                    assistantConversationService.updateProcessingStatus(memberId, serverId, false, memberId);
                });
    }

    /**
     * Extracts the content field from the agent's JSON response.
     * <p>
     * The agent returns JSON responses in the format: {"content": "...", "tool_name": "...", "type": "..."}
     * This method parses the JSON and returns only the content portion.
     * </p>
     *
     * @param jsonResponse The raw JSON response from the agent
     * @return The content string, or empty string if parsing fails or content is empty
     */
    private String extractContentFromAgentResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return "";
        }

        try {
            AgentResponseDto agentResponse = objectMapper.readValue(jsonResponse.trim(), AgentResponseDto.class);
            String content = agentResponse.getContent();
            
            // Return content or empty string if content is null/empty
            return (content != null && !content.trim().isEmpty()) ? content : "";
            
        } catch (Exception e) {
            log.warn("Failed to parse agent response as JSON, treating as plain text: [{}]. Error: {}", 
                    jsonResponse.replace("\n", "\\n").replace("\r", "\\r"), e.getMessage());
            
            // Fallback: if it's not valid JSON, return the original response
            // This handles cases where the agent might send non-JSON content
            return jsonResponse.trim().isEmpty() ? "" : jsonResponse;
        }
    }
}
