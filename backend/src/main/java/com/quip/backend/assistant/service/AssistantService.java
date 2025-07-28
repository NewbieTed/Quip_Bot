package com.quip.backend.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.dto.response.AgentResponseDto;
import com.quip.backend.assistant.dto.response.AgentToolDto;
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
    private final ToolMapper toolMapper;
    private final ToolWhitelistService toolWhitelistService;

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
                .doOnNext(chunk -> log.info("Received raw chunk from agent: [{}]", chunk.replace("\n", "\\n").replace("\r", "\\r")))
                .map(this::extractContentFromAgentResponse)
                .filter(content -> !content.isEmpty())
                .doOnNext(content -> log.info("Extracted content: [{}]", content.replace("\n", "\\n").replace("\r", "\\r")))
                .doOnError(error -> log.error("Error communicating with agent: {}", error.getMessage(), error))
                .doOnComplete(() -> log.info("Agent response stream completed"));
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

        // Get whitelisted tools for this member and server context
        // TODO: Add conversation ID support when available in the request
        List<String> whitelistedTools = getWhitelistedToolsForMember(memberId, serverId, null);

        // Prepare the payload with all necessary context for the AI assistant
        Map<String, Object> payload = Map.of(
                "message", assistantRequestDto.getMessage(),
                "serverId", serverId,
                "channelId", channelId,
                "memberId", memberId,
                "whitelistedTools", whitelistedTools
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
                .doOnNext(chunk -> log.info("Received raw chunk from agent: [{}]", chunk.replace("\n", "\\n").replace("\r", "\\r")))
                .map(this::extractContentFromAgentResponse)
                .filter(content -> !content.isEmpty())
                .doOnNext(content -> log.info("Extracted content: [{}]", content.replace("\n", "\\n").replace("\r", "\\r")))
                .doOnError(error -> log.error("Error communicating with agent: {}", error.getMessage(), error))
                .doOnComplete(() -> log.info("Agent response stream completed"));
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

    /**
     * Gets the list of whitelisted tool names for a specific member and server context.
     * <p>
     * This method retrieves tools that are whitelisted for the member at different scopes:
     * - Global scope: Tools available to the member across all servers
     * - Server scope: Tools available to the member within the specific server
     * - Conversation scope: Tools available to the member within a specific conversation (if provided)
     * </p>
     * <p>
     * This method uses the cached ToolWhitelistService to improve performance for
     * frequently accessed tool whitelist data.
     * </p>
     *
     * @param memberId The ID of the member
     * @param serverId The ID of the server
     * @param conversationId The ID of the conversation (optional, can be null)
     * @return List of whitelisted tool names
     */
    private List<String> getWhitelistedToolsForMember(Long memberId, Long serverId, Long conversationId) {
        try {
            // Get all active whitelist entries for the member and server using cached service
            List<ToolWhitelist> whitelistEntries = toolWhitelistService.getActiveToolWhitelistByMemberAndServer(memberId, serverId);
            
            // Filter entries based on scope and conversation context
            Set<Long> whitelistedToolIds = whitelistEntries.stream()
                    .filter(entry -> isEntryApplicable(entry, conversationId))
                    .map(ToolWhitelist::getToolId)
                    .collect(Collectors.toSet());
            
            if (whitelistedToolIds.isEmpty()) {
                log.debug("No whitelisted tools found for member {} in server {}", memberId, serverId);
                return List.of();
            }
            
            // Get the actual tool names from the tool IDs
            List<Tool> tools = toolMapper.selectBatchIds(whitelistedToolIds);
            List<String> toolNames = tools.stream()
                    .filter(tool -> tool.getEnabled() != null && tool.getEnabled())
                    .map(Tool::getToolName)
                    .collect(Collectors.toList());
            
            log.info("Found {} whitelisted tools for member {} in server {}: {}", 
                    toolNames.size(), memberId, serverId, toolNames);
            
            return toolNames;
            
        } catch (Exception e) {
            log.error("Error retrieving whitelisted tools for member {} in server {}: {}", 
                    memberId, serverId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Checks if a whitelist entry is applicable for the current context.
     * <p>
     * An entry is applicable if:
     * - It's a GLOBAL scope entry
     * - It's a SERVER scope entry
     * - It's a CONVERSATION scope entry and the conversation ID matches
     * </p>
     *
     * @param entry The whitelist entry to check
     * @param conversationId The current conversation ID (can be null)
     * @return true if the entry is applicable, false otherwise
     */
    private boolean isEntryApplicable(ToolWhitelist entry, Long conversationId) {
        ToolWhitelistScope scope = entry.getScope();
        
        switch (scope) {
            case GLOBAL:
            case SERVER:
                return true;
            case CONVERSATION:
                return conversationId != null && conversationId.equals(entry.getAgentConversationId());
            default:
                log.warn("Unknown whitelist scope: {}", scope);
                return false;
        }
    }

    /**
     * Fetches the list of available tools from the agent.
     * <p>
     * This method can be used to discover what tools are currently available
     * on the agent side, which can help with tool synchronization and management.
     * This is useful for administrative purposes and tool discovery.
     * </p>
     *
     * @return A Mono containing the list of available tools from the agent
     */
    public Mono<List<AgentToolDto>> getAvailableToolsFromAgent() {
        log.info("Fetching available tools from agent");
        
        return webClient
                .get()
                .uri(AGENT_BASE_URL + "/tools")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AgentToolDto>>() {})
                .doOnNext(tools -> log.info("Retrieved {} tools from agent", tools.size()))
                .doOnError(error -> log.error("Error fetching tools from agent: {}", error.getMessage(), error))
                .onErrorReturn(List.of()); // Return empty list on error
    }
}
