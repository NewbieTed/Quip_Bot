package com.quip.backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.config.SpringConfigurator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocket endpoint for handling assistant invocation requests.
 * Provides real-time streaming of assistant responses through WebSocket connections.
 */
@Setter
@Slf4j
@Component
@ServerEndpoint(value = "/assistant/invoke", configurator = SpringConfigurator.class)
public class AssistantWebSocketEndpoint {

    // Setters for dependency injection (used by the injector)
    @Autowired
    private AssistantService assistantService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    /**
     * Default constructor for WebSocket container instantiation.
     */
    public AssistantWebSocketEndpoint() {
        // Default constructor required by WebSocket container
    }

    /**
     * Constructor for dependency injection (used in tests and manual instantiation).
     */
    public AssistantWebSocketEndpoint(AssistantService assistantService, ObjectMapper objectMapper, Validator validator) {
        this.assistantService = assistantService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Handles WebSocket connection opening.
     *
     * @param session the WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        log.info("WebSocket opened: {}", session.getId());
    }

    /**
     * Handles incoming WebSocket messages.
     * Validates the request, invokes the assistant service, and streams responses back.
     *
     * @param message the incoming message
     * @param session the WebSocket session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        if (assistantService == null || objectMapper == null || validator == null) {
            sendErrorAndClose(session, "Service dependencies not properly initialized");
            return;
        }

        try {
            AssistantRequestDto dto = parseMessage(message);
            Set<ConstraintViolation<AssistantRequestDto>> violations = validateRequest(dto);

            if (!violations.isEmpty()) {
                String errorMessage = formatValidationErrors(violations);
                sendErrorAndClose(session, "Validation error: " + errorMessage);
                return;
            }

            processAssistantRequest(dto, session);

        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage(), e);
            sendErrorAndClose(session, "Error parsing message: " + e.getMessage());
        }
    }

    /**
     * Handles WebSocket connection closing.
     *
     * @param session the WebSocket session
     */
    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket closed: {}", session.getId());
    }

    /**
     * Handles WebSocket errors.
     *
     * @param session   the WebSocket session
     * @param throwable the error that occurred
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error on session {}: {}", session.getId(), throwable.getMessage(), throwable);
    }

    /**
     * Parses the incoming message into an AssistantRequestDto.
     */
    private AssistantRequestDto parseMessage(String message) throws Exception {
        return objectMapper.readValue(message, AssistantRequestDto.class);
    }

    /**
     * Validates the assistant request DTO.
     */
    private Set<ConstraintViolation<AssistantRequestDto>> validateRequest(AssistantRequestDto dto) {
        return validator.validate(dto);
    }

    /**
     * Formats validation errors into a readable string.
     */
    private String formatValidationErrors(Set<ConstraintViolation<AssistantRequestDto>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
    }

    /**
     * Processes the assistant request and handles the streaming response.
     */
    private void processAssistantRequest(AssistantRequestDto dto, Session session) {
        try {
            Flux<String> responseFlux = assistantService.invokeAssistant(dto);
            
            responseFlux
                .doOnComplete(() -> closeSessionSafely(session, "completion"))
                .subscribe(
                    chunk -> sendChunkSafely(session, chunk),
                    error -> handleStreamError(session, error)
                );
                
        } catch (Exception e) {
            log.error("Error processing assistant request: {}", e.getMessage(), e);
            sendErrorAndClose(session, "Error processing request: " + e.getMessage());
        }
    }

    /**
     * Safely sends a chunk of data to the WebSocket session.
     */
    private void sendChunkSafely(Session session, String chunk) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(chunk);
            }
        } catch (IOException e) {
            log.error("Error sending chunk to session {}: {}", session.getId(), e.getMessage(), e);
        }
    }

    /**
     * Handles errors in the response stream.
     */
    private void handleStreamError(Session session, Throwable error) {
        log.error("Error in assistant response stream for session {}: {}", session.getId(), error.getMessage(), error);
        sendErrorAndClose(session, "Error: " + error.getMessage());
    }

    /**
     * Sends an error message and closes the session.
     */
    private void sendErrorAndClose(Session session, String errorMessage) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(errorMessage);
                session.close();
            }
        } catch (IOException e) {
            log.error("Error sending error message and closing session {}: {}", session.getId(), e.getMessage(), e);
        }
    }

    /**
     * Safely closes the WebSocket session.
     */
    private void closeSessionSafely(Session session, String reason) {
        try {
            if (session.isOpen()) {
                session.close();
                log.info("WebSocket session {} closed after {}.", session.getId(), reason);
            }
        } catch (IOException e) {
            log.error("Error closing WebSocket session {}: {}", session.getId(), e.getMessage(), e);
        }
    }

}
