
package com.quip.backend.problem.service;

import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemCategoryRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemCategoryResponseDtoMapper;
import com.quip.backend.problem.model.ProblemCategory;
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
public class ProblemCategoryServiceTest extends BaseTest {

    @InjectMocks
    private ProblemCategoryService problemCategoryService;

    @Mock
    private MemberService memberService;

    @Mock
    private ServerService serverService;

    @Mock
    private ChannelService channelService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ProblemCategoryMapper problemCategoryMapper;

    @Mock
    private GetProblemCategoryResponseDtoMapper getProblemCategoryResponseDtoMapper;

    @Mock
    private CreateProblemCategoryRequestDtoMapper createProblemCategoryRequestDtoMapper;

    @Test
    void getServerProblemCategories_validRequest_returnsListOfCategories() {
        long memberId = 1L;
        long channelId = 1L;
        long serverId = 1L;

        GetProblemCategoryRequestDto requestDto = new GetProblemCategoryRequestDto();
        requestDto.setMemberId(memberId);
        requestDto.setChannelId(channelId);

        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        when(channelService.findServerId(anyLong())).thenReturn(serverId);
        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(serverService).validateServer(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());

        ProblemCategory problemCategory = new ProblemCategory();
        when(problemCategoryMapper.selectByServerId(serverId)).thenReturn(Collections.singletonList(problemCategory));

        GetProblemCategoryResponseDto responseDto = new GetProblemCategoryResponseDto();
        when(getProblemCategoryResponseDtoMapper.toProblemCategoryDto(problemCategory)).thenReturn(responseDto);

        List<GetProblemCategoryResponseDto> result = problemCategoryService.getServerProblemCategories(requestDto);

        assertFalse(result.isEmpty());
    }

    @Test
    void addProblemCategory_validRequest_noException() {
        long memberId = 1L;
        long channelId = 1L;
        long serverId = 1L;
        String categoryName = "Test Category";
        String categoryDescription = "Test Description";

        CreateProblemCategoryRequestDto requestDto = new CreateProblemCategoryRequestDto();
        requestDto.setMemberId(memberId);
        requestDto.setChannelId(channelId);
        requestDto.setProblemCategoryName(categoryName);
        requestDto.setProblemCategoryDescription(categoryDescription);

        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        when(channelService.findServerId(anyLong())).thenReturn(serverId);
        doNothing().when(serverService).validateServer(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());

        ProblemCategory problemCategory = new ProblemCategory();
        when(createProblemCategoryRequestDtoMapper.toProblemCategory(requestDto)).thenReturn(problemCategory);

        assertDoesNotThrow(() -> {
            problemCategoryService.addProblemCategory(requestDto);
        });
    }

    @Test
    void addProblemCategory_invalidName_throwsException() {
        CreateProblemCategoryRequestDto requestDto = new CreateProblemCategoryRequestDto();
        requestDto.setMemberId(1L);
        requestDto.setChannelId(1L);
        requestDto.setProblemCategoryName(""); // Invalid name
        requestDto.setProblemCategoryDescription("Test Description");

        doNothing().when(memberService).validateMember(anyLong(), anyString());
        doNothing().when(channelService).validateChannel(anyLong(), anyString());
        when(channelService.findServerId(anyLong())).thenReturn(1L);
        doNothing().when(serverService).validateServer(anyLong(), anyString());
        doNothing().when(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());

        assertThrows(ValidationException.class, () -> {
            problemCategoryService.addProblemCategory(requestDto);
        });
    }

    @Test
    void validateProblemCategory_existingCategory_noException() {
        long categoryId = 1L;
        when(problemCategoryMapper.selectById(categoryId)).thenReturn(new ProblemCategory());

        assertDoesNotThrow(() -> {
            problemCategoryService.validateProblemCategory(categoryId, "testOperation");
        });
    }

    @Test
    void validateProblemCategory_nonExistingCategory_throwsException() {
        long categoryId = 1L;
        when(problemCategoryMapper.selectById(categoryId)).thenReturn(null);

        assertThrows(ValidationException.class, () -> {
            problemCategoryService.validateProblemCategory(categoryId, "testOperation");
        });
    }
}
