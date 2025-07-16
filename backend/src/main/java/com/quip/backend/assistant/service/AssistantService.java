package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.server.service.ServerService;
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
    private final MemberService memberService;
    private final ChannelService channelService;
    private final ServerService serverService;
    private final AuthorizationService authorizationService;
    private final ReactorNettyWebSocketClient webSocketClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INVOKE_ASSISTANT = "Invoke Assistant";

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

        return Flux.create(sink ->
            webSocketClient.execute(
                URI.create("ws://host.docker.internal:5001/assistant"),
                session -> {
                    try {
                        String payload = objectMapper.writeValueAsString(
                            Map.of(
                                    "message", assistantRequestDto.getMessage(),
                                    "serverId", serverId,
                                    "channelId", channelId,
                                    "memberId", memberId
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
