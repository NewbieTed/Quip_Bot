package com.quip.backend.assistant.controller;

import com.quip.backend.assistant.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI assistant functionality.
 * <p>
 * This controller provides endpoints for interacting with the AI assistant.
 * It handles HTTP requests related to assistant invocation and delegates
 * business logic to the AssistantService.
 * </p>
 * <p>
 * Note: The invoke endpoint is currently commented out and may be implemented
 * in a future update.
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

//    @PostMapping("/invoke")
//    public Flux<String> invokeAssistant(@Valid @RequestBody AssistantRequestDto assistantRequestDto) {
//        return assistantService.invokeAssistant(assistantRequestDto);
//    }
}
