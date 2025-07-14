package com.quip.backend.assistant.controller;

import com.quip.backend.assistant.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

//    @PostMapping("/invoke")
//    public Flux<String> invokeAssistant(@Valid @RequestBody AssistantRequestDto assistantRequestDto) {
//        return assistantService.invokeAssistant(assistantRequestDto);
//    }
}
