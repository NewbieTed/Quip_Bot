package com.quip.backend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.dto.AssistantRequestDto;
import com.quip.backend.assistant.service.AssistantService;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@ServerEndpoint("/assistant/invoke")
public class AssistantWebSocketEndpoint {

    @Setter
    private static AssistantService assistantService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) {
        log.info("WebSocket opened: {}", session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            AssistantRequestDto dto = objectMapper.readValue(message, AssistantRequestDto.class);
            Flux<String> flux = assistantService.invokeAssistant(dto);
            flux.doOnComplete(() -> {
                try {
                    session.close();
                    log.info("WebSocket session {} closed after completion.", session.getId());
                } catch (Exception e) {
                    log.error("Error closing WebSocket session", e);
                }
            })
            .subscribe(
                chunk -> {
                    try {
                        session.getBasicRemote().sendText(chunk);
                    } catch (Exception e) {
                        log.error("Error sending chunk", e);
                    }
                },
                error -> {
                    try {
                        session.getBasicRemote().sendText("Error: " + error.getMessage());
                        session.close();
                    } catch (Exception e) {
                        log.error("Error sending error and closing session", e);
                    }
                }
            );
        } catch (Exception e) {
            try {
                session.getBasicRemote().sendText("Error parsing message: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Error sending parse error", ex);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket closed: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error on session {}: {}", session.getId(), throwable.getMessage(), throwable);
    }
}