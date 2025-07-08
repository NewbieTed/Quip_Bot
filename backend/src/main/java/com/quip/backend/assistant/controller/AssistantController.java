package com.quip.backend.assistant.controller;

import com.quip.backend.assistant.dto.AssistantRequestDto;
import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.dto.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/invoke")
    public Flux<String> invokeAssistant(@Valid @RequestBody AssistantRequestDto assistantRequestDto) {
        return assistantService.invokeAssistant(assistantRequestDto);
    }
}
