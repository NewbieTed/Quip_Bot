
package com.quip.backend.assistant.service;

import com.quip.backend.assistant.dto.request.AssistantRequestDto;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.server.service.ServerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssistantServiceTest extends BaseTest {

    @InjectMocks
    private AssistantService assistantService;

    @Mock
    private MemberService memberService;

    @Mock
    private ChannelService channelService;

    @Mock
    private ServerService serverService;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void invokeAssistant_validRequest_returnsFlux() {
        long memberId = 1L;
        long channelId = 1L;
        long serverId = 1L;
        String message = "Hello, Assistant!";

        AssistantRequestDto requestDto = new AssistantRequestDto();
        requestDto.setMemberId(memberId);
        requestDto.setChannelId(channelId);
        requestDto.setMessage(message);

        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        when(channelService.findServerId(anyLong())).thenReturn(serverId);
        doNothing().when(serverService).validateServer(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());

        Flux<String> result = assistantService.invokeAssistant(requestDto);

        // Since we can't test the WebSocket connection in a unit test, 
        // we'll just verify that the Flux is created without errors.
        StepVerifier.create(result)
                .expectNextCount(0)
                .expectTimeout(Duration.ofSeconds(1))
                .verify();
    }
}
