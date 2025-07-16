package com.quip.backend.problem.service;

import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;

import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemCategoryRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemCategoryResponseDtoMapper;
import com.quip.backend.problem.model.ProblemCategory;
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
    private final AuthorizationService authorizationService;

    private final ProblemCategoryMapper problemCategoryMapper;

    private final GetProblemCategoryResponseDtoMapper getProblemCategoryResponseDtoMapper;
    private final CreateProblemCategoryRequestDtoMapper createProblemCategoryRequestDtoMapper;

    private static final String CREATE_PROBLEM_CATEGORY = "Problem Category Creation";
    private static final String GET_PROBLEM_CATEGORIES = "Problem Category Retrieval";

    public List<GetProblemCategoryResponseDto> getServerProblemCategories(GetProblemCategoryRequestDto getProblemCategoryRequestDto) {
        // Validate authorization
        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                getProblemCategoryRequestDto.getMemberId(),
                getProblemCategoryRequestDto.getChannelId(),
                AuthorizationConstants.VIEW_PROBLEM_CATEGORY,
                GET_PROBLEM_CATEGORIES
        );

        // Compile problem categories
        List<ProblemCategory> problemCategories = problemCategoryMapper.selectByServerId(authorizationContext.server().getId());
        List<GetProblemCategoryResponseDto> getProblemCategoryResponseDtos = new ArrayList<>();
        for (ProblemCategory problemCategory : problemCategories) {
            getProblemCategoryResponseDtos.add(getProblemCategoryResponseDtoMapper.toProblemCategoryDto(problemCategory));
        }

        return getProblemCategoryResponseDtos;
    }

    public void addProblemCategory(CreateProblemCategoryRequestDto createProblemCategoryRequestDto) {
        // Validate authorization
        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                createProblemCategoryRequestDto.getMemberId(),
                createProblemCategoryRequestDto.getChannelId(),
                AuthorizationConstants.MANAGE_PROBLEM_CATEGORY,
                CREATE_PROBLEM_CATEGORY
        );

        ProblemCategory problemCategory = createProblemCategoryRequestDtoMapper.toProblemCategory(createProblemCategoryRequestDto);
        problemCategory.setServerId(authorizationContext.server().getId());

        validateNewProblemCategory(problemCategory);

        problemCategoryMapper.insert(problemCategory);
    }


    public ProblemCategory validateProblemCategory(Long problemCategoryId, String operation) {
        if (problemCategoryId == null) {
            throw new ValidationException(operation, "problemCategoryId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }

        ProblemCategory problemCategory = problemCategoryMapper.selectById(problemCategoryId);
        if (problemCategory == null) {
            throw new ValidationException(operation, "problemCategoryId", "must refer to an existing problem category");
        }
        log.info("Validated problemCategoryId: {}", problemCategoryId);
        return problemCategory;
    }

    private void validateNewProblemCategory(ProblemCategory problemCategory) {
        String problemCategoryName = problemCategory.getCategoryName();
        String problemCategoryDescription = problemCategory.getDescription();

        if (problemCategoryName == null || problemCategoryName.trim().isEmpty()) {
            throw new ValidationException(CREATE_PROBLEM_CATEGORY, "problemCategoryName", "must not be empty");
        }

        if (problemCategoryDescription == null || problemCategoryDescription.trim().isEmpty()) {
            throw new ValidationException(CREATE_PROBLEM_CATEGORY, "problemCategoryDescription", "must not be empty");
        }
        log.info("Validated new problemCategory: {}", problemCategoryName);
    }
}
