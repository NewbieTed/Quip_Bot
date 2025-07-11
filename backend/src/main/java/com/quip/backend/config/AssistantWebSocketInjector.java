package com.quip.backend.config;

import com.quip.backend.assistant.service.AssistantService;
import com.quip.backend.ws.AssistantWebSocketEndpoint;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AssistantWebSocketInjector {
    private final AssistantService assistantService;

    @PostConstruct
    public void init() {
        AssistantWebSocketEndpoint.setAssistantService(assistantService);
    }
}
