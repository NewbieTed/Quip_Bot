package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.AssistantRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.HashMap;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://host.docker.internal:5001")
            .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<String> invokeAssistant(AssistantRequestDto assistantRequestDto) {
        String message = assistantRequestDto.getMessage();
        Long memberId = assistantRequestDto.getMemberId();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", message);
        requestBody.put("memberId", memberId);

        return webClient.post()
                .uri("/assistant")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .accept(MediaType.APPLICATION_NDJSON)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    try {
                        JsonNode node = objectMapper.readTree(chunk);
                        return node.has("response") ? node.get("response").asText() : chunk;
                    } catch (Exception e) {
                        log.error("Error parsing assistant chunk: {}", e.getMessage(), e);
                        return chunk;
                    }
                })
                .doOnNext(chunk -> log.info("Assistant result extracted: {}", chunk))
                .doOnError(e -> log.error("Error invoking assistant: {}", e.getMessage(), e));
    }
}
