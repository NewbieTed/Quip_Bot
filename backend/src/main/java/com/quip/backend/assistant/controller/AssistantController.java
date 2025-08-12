package com.quip.backend.assistant.controller;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST controller for AI assistant functionality.
 * <p>
 * This controller provides endpoints for interacting with the AI assistant.
 * It handles HTTP requests related to assistant invocation and delegates
 * business logic to the AssistantService.
 * </p>
 */
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    /**
     * Service for AI assistant-related operations.
     */
    private final AssistantService assistantService;

    /**
     * Invokes the AI assistant and streams the response back to the client.
     * <p>
     * This endpoint accepts a POST request with assistant parameters and returns
     * a streaming response from the AI agent. The response is streamed as
     * Server-Sent Events (SSE) to allow real-time communication.
     * </p>
     *
     * @param assistantRequestDto The request containing member ID, channel ID, and message
     * @return A Flux of string responses from the assistant as streaming HTTP
     */
    @PostMapping(value = "/invoke", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> invokeAssistant(@Valid @RequestBody AssistantRequestDto assistantRequestDto) {
        return assistantService.invokeAssistant(assistantRequestDto);
    }


    @PostMapping(value = "/approve")
    public BaseResponse<Boolean> processDecision(@Valid @RequestBody AssistantRequestDto assistantRequestDto) {
        assistantService.invokeAssistant(assistantRequestDto);
        return BaseResponse.success();
    }
}
