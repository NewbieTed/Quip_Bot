package com.quip.backend.problem.service;

import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemCategoryService {
    // Service modules
    private final MemberService memberService;
    private final ServerService serverService;
    private final ChannelService channelService;
    private final AuthorizationService authorizationService;

    private final ProblemCategoryMapper problemCategoryMapper;

    private final GetProblemCategoryResponseDtoMapper getProblemCategoryResponseDtoMapper;
    private final CreateProblemCategoryRequestDtoMapper createProblemCategoryRequestDtoMapper;

    private static final String CREATE_CATEGORY = "Category Creation";
    private static final String GET_CATEGORIES = "Category Retrieval";

    public List<GetProblemCategoryResponseDto> getServerProblemCategories(GetProblemCategoryRequestDto getProblemCategoryRequestDto) {
        Long memberId = getProblemCategoryRequestDto.getMemberId();
        Long channelId = getProblemCategoryRequestDto.getChannelId();;
        channelService.validateChannel(channelId, GET_CATEGORIES);
        Long serverId = channelService.findServerId(channelId);
        memberService.validateMember(memberId, GET_CATEGORIES);

        // Validate server, this can be simplified to a null check as the DB enforces the validity of the server
        serverService.validateServer(serverId, GET_CATEGORIES);

        // Validate authorization
        authorizationService.validateAuthorization(
                memberId,
                channelId,
                AuthorizationConstants.VIEW_PROBLEM_CATEGORY,
                GET_CATEGORIES
        );

        // Compile problem categories
        List<ProblemCategory> problemCategories = problemCategoryMapper.selectByServerId(serverId);
        List<GetProblemCategoryResponseDto> getProblemCategoryResponseDtos = new ArrayList<>();
        for (ProblemCategory problemCategory : problemCategories) {
            getProblemCategoryResponseDtos.add(getProblemCategoryResponseDtoMapper.toProblemCategoryDto(problemCategory));
        }

        return getProblemCategoryResponseDtos;
    }

    public void addProblemCategory(CreateProblemCategoryRequestDto createProblemCategoryRequestDto) {
        Long memberId = createProblemCategoryRequestDto.getMemberId();
        Long channelId = createProblemCategoryRequestDto.getChannelId();
        String problemCategoryName = createProblemCategoryRequestDto.getProblemCategoryName();
        String problemCategoryDescription = createProblemCategoryRequestDto.getProblemCategoryDescription();
        memberService.validateMember(memberId, CREATE_CATEGORY);
        channelService.validateChannel(channelId, CREATE_CATEGORY);
        Long serverId = channelService.findServerId(channelId);

        // Validate server, this can be simplified to a null check as the DB enforces the validity of the server
        serverService.validateServer(serverId, CREATE_CATEGORY);

        // Validate authorization
        authorizationService.validateAuthorization(
                memberId,
                channelId,
                AuthorizationConstants.MANAGE_PROBLEM_CATEGORY,
                CREATE_CATEGORY
        );

        if (problemCategoryName == null || problemCategoryName.trim().isEmpty()) {
            throw new ValidationException(CREATE_CATEGORY, "problemCategoryName", "must not be empty");
        }

        if (problemCategoryDescription == null || problemCategoryDescription.trim().isEmpty()) {
            throw new ValidationException(CREATE_CATEGORY, "problemCategoryName", "must not be empty");
        }

        ProblemCategory problemCategory = createProblemCategoryRequestDtoMapper.toProblemCategory(createProblemCategoryRequestDto);
        problemCategory.setServerId(serverId);
        problemCategoryMapper.insert(problemCategory);
    }


    public boolean isProblemCategoryExists(Long problemCategoryId) {
        ProblemCategory problemCategory = problemCategoryMapper.selectById(problemCategoryId);
        return problemCategory != null;
    }

    public void validateProblemCategory(Long problemCategoryId, String operation) {
        if (!this.isProblemCategoryExists(problemCategoryId)) {
            throw new ValidationException(operation, "problemCategoryId", "must refer to an existing problem category");
        }
        log.info("Validated problemCategoryId: {}", problemCategoryId);
    }


}
