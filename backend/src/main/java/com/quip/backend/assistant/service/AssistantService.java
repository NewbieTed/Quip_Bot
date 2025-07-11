package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.AssistantRequestDto;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<String> invokeAssistant(AssistantRequestDto assistantRequestDto) {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        return Flux.create(sink ->
            client.execute(
                URI.create("ws://host.docker.internal:5001/assistant"),
                session -> {
                    try {
                        String payload = objectMapper.writeValueAsString(
                            Map.of(
                                "message", assistantRequestDto.getMessage(),
                                "memberId", assistantRequestDto.getMemberId()
                            )
                        );
                        return session.send(Mono.just(session.textMessage(payload)))
                            .thenMany(session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(message -> {
                                    log.info("Received chunk: {}", message);
                                    sink.next(message);
                                })
                                .doOnError(e -> {
                                    log.error("WebSocket receive error: {}", e.getMessage(), e);
                                    sink.error(e);
                                })
                                .then())
                            .doFinally(signal -> {
                                log.info("WebSocket session closed with signal: {}", signal);
                                sink.complete();
                            })
                            .then();
                    } catch (Exception e) {
                        log.error("WebSocket initialization error: {}", e.getMessage(), e);
                        sink.error(e);
                        return Mono.empty();
                    }
                }
            ).subscribe()
        );
    }
}
