package com.quip.backend.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://host.docker.internal:5001")
            .build();

    public String invokeAssistant(String message) {
        try {
            String result = webClient.post()
                    .uri("/assistant")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(java.util.Map.of("message", message))
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(chunk -> log.info("Assistant result: {}", chunk))
                    .doOnError(e -> log.error("Error invoking assistant: {}", e.getMessage(), e))
                    .block();

            return result != null ? result : "No response from assistant.";

        } catch (Exception e) {
            log.error("Error invoking assistant: {}", e.getMessage(), e);
            return "Error invoking assistant: " + e.getMessage();
        }
    }
}
