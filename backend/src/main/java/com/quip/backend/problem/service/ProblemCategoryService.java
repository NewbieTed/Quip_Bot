package com.quip.backend.problem.service;

import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;

import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
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
    private ServerService serverService;
    private ChannelService channelService;

    private ProblemCategoryMapper problemCategoryMapper;
    private GetProblemCategoryResponseDtoMapper getProblemCategoryResponseDtoMapper;

    private static final String GET_CATEGORIES = "Category Retrieval";

    public List<GetProblemCategoryResponseDto> getServerProblemCategories(GetProblemCategoryRequestDto getProblemCategoryRequestDto) {
        Long channelId = getProblemCategoryRequestDto.getChannelId();;
        Long serverId = channelService.findServerId(channelId);

        // Validate server, this can be simplified to a null check as the DB enforces the validity of the server
        serverService.validateServer(serverId, GET_CATEGORIES);

        // Compile problem categories
        List<ProblemCategory> problemCategories = problemCategoryMapper.selectByServerId(serverId);
        List<GetProblemCategoryResponseDto> getProblemCategoryResponseDtos = new ArrayList<>();
        for (ProblemCategory problemCategory : problemCategories) {
            getProblemCategoryResponseDtos.add(getProblemCategoryResponseDtoMapper.toProblemCategoryDto(problemCategory));
        }

        return getProblemCategoryResponseDtos;
    }


    public void validateExists(Long problemCategoryId, String operation) {
        if (problemCategoryMapper.selectById(problemCategoryId) == null) {
            throw new ValidationException(operation, "categoryId", "must refer to an existing problem category");
        }
        log.info("Validated problemCategoryId: {}", problemCategoryId);
    }
}
