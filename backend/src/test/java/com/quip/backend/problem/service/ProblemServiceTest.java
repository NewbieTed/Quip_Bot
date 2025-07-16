
package com.quip.backend.problem.service;

import com.quip.backend.asset.utils.AssetUtils;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.dto.request.GetProblemRequestDto;
import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemListItemResponseDtoMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.server.service.ServerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProblemServiceTest extends BaseTest {

    @InjectMocks
    private ProblemService problemService;

    @Mock
    private AssetUtils assetUtils;

    @Mock
    private ProblemCategoryService problemCategoryService;

    @Mock
    private ProblemChoiceService problemChoiceService;

    @Mock
    private MemberService memberService;

    @Mock
    private ChannelService channelService;

    @Mock
    private ServerService serverService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private CreateProblemRequestDtoMapper createProblemRequestDtoMapper;

    @Mock
    private GetProblemListItemResponseDtoMapper getProblemListItemResponseDtoMapper;

    @Test
    void getProblemsByCategory_validRequest_returnsListOfProblems() {
        long memberId = 1L;
        long channelId = 1L;
        long problemCategoryId = 1L;

        GetProblemRequestDto requestDto = new GetProblemRequestDto();
        requestDto.setMemberId(memberId);
        requestDto.setChannelId(channelId);
        requestDto.setProblemCategoryId(problemCategoryId);

        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        doNothing().when(problemCategoryService).validateProblemCategory(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());

        Problem problem = new Problem();
        when(problemMapper.selectByProblemCategoryId(problemCategoryId)).thenReturn(Collections.singletonList(problem));

        GetProblemListItemResponseDto responseDto = new GetProblemListItemResponseDto();
        when(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(problem)).thenReturn(responseDto);

        List<GetProblemListItemResponseDto> result = problemService.getProblemsByCategory(requestDto);

        assertFalse(result.isEmpty());
    }

    @Test
    void addProblem_validRequest_noException() {
        long memberId = 1L;
        long channelId = 1L;
        long serverId = 1L;
        long problemCategoryId = 1L;
        String question = "Test Question";

        CreateProblemRequestDto requestDto = new CreateProblemRequestDto();
        requestDto.setMemberId(memberId);
        requestDto.setChannelId(channelId);
        requestDto.setProblemCategoryId(problemCategoryId);
        requestDto.setQuestion(question);

        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        when(channelService.findServerId(anyLong())).thenReturn(serverId);
        doNothing().when(serverService).validateServer(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());
        doNothing().when(problemCategoryService).validateProblemCategory(anyLong(), anyString());
        doNothing().when(problemChoiceService).validateProblemChoices(any(), anyString());

        Problem problem = new Problem();
        problem.setId(1L); // Simulate successful insertion
        when(createProblemRequestDtoMapper.toProblem(requestDto)).thenReturn(problem);

        assertDoesNotThrow(() -> {
            problemService.addProblem(requestDto);
        });
    }

    @Test
    void addProblem_invalidQuestion_throwsException() {
        CreateProblemRequestDto requestDto = new CreateProblemRequestDto();
        requestDto.setMemberId(1L);
        requestDto.setChannelId(1L);
        requestDto.setProblemCategoryId(1L);
        requestDto.setQuestion(""); // Invalid question

        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        when(channelService.findServerId(anyLong())).thenReturn(1L);
        doNothing().when(serverService).validateServer(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());
        doNothing().when(problemCategoryService).validateProblemCategory(anyLong(), anyString());

        assertThrows(ValidationException.class, () -> {
            problemService.addProblem(requestDto);
        });
    }

    @Test
    void validateProblem_validQuestion_noException() {
        assertDoesNotThrow(() -> {
            problemService.validateProblem("Valid Question", "testOperation");
        });
    }

    @Test
    void validateProblem_invalidQuestion_throwsException() {
        assertThrows(ValidationException.class, () -> {
            problemService.validateProblem("", "testOperation");
        });
    }
}
