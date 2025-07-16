package com.quip.backend.problem.service;

import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.dto.request.GetProblemRequestDto;
import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.dto.response.GetProblemResponseDto;
import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.database.ProblemChoiceMapper;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemChoiceRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemListItemResponseDtoMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.model.ProblemCategory;
import com.quip.backend.problem.model.ProblemChoice;
import com.quip.backend.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

//    private final AssetUtils assetUtils;

    // Service modules
    private final ProblemCategoryService problemCategoryService;
    private final ProblemChoiceService problemChoiceService;
    private final MemberService memberService;
    private final ChannelService channelService;
    private final ServerService serverService;
    private final AuthorizationService authorizationService;

    // Mybatis mappers
    private final ProblemMapper problemMapper;
    private final ProblemChoiceMapper problemChoiceMapper;
    private final ProblemCategoryMapper problemCategoryMapper;

    // Mapstruct mappers
    private final CreateProblemRequestDtoMapper createProblemRequestDtoMapper;
    private final CreateProblemChoiceRequestDtoMapper createProblemChoiceRequestDtoMapper;
    private final GetProblemListItemResponseDtoMapper getProblemListItemResponseDtoMapper;

    private static final String CREATE_PROBLEM = "Problem Creation";
    private static final String CREATE_PROBLEM_CHOICE = "Problem Choice Creation";
    private static final String RETRIEVE_PROBLEM = "Problem Retrieval";

    public GetProblemResponseDto getProblem() {
        return null; // Placeholder for future implementation
    }

    // TODO: Change other feature's file structure on DTOs as well as naming

    public List<GetProblemListItemResponseDto> getProblemsByCategory(GetProblemRequestDto getProblemRequestDto) {
        authorizationService.validateAuthorization(
                getProblemRequestDto.getMemberId(),
                getProblemRequestDto.getChannelId(),
                AuthorizationConstants.VIEW_PROBLEM,
                RETRIEVE_PROBLEM
        );
        ProblemCategory problemCategory = problemCategoryService.validateProblemCategory(getProblemRequestDto.getProblemCategoryId(), RETRIEVE_PROBLEM);

        List<Problem> problems = problemMapper.selectByProblemCategoryId(problemCategory.getId());
        List<GetProblemListItemResponseDto> getProblemResponseDtos = new ArrayList<>();

        for (Problem problem : problems) {
            getProblemResponseDtos.add(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(problem));
        }

        return getProblemResponseDtos;
    }

    @Transactional
    public void addProblem(CreateProblemRequestDto problemCreateDto) {
        if (problemCreateDto == null) {
            throw new ValidationException(CREATE_PROBLEM, "body", "must not be null");
        }

        // Validate authorization
        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                problemCreateDto.getMemberId(),
                problemCreateDto.getChannelId(),
                AuthorizationConstants.MANAGE_PROBLEM,
                CREATE_PROBLEM
        );

        // Validate fields
        problemCategoryService.validateProblemCategory(problemCreateDto.getProblemCategoryId(), CREATE_PROBLEM);
        this.validateProblem(problemCreateDto.getQuestion(), CREATE_PROBLEM);
        problemChoiceService.validateProblemChoices(problemCreateDto.getChoices(), CREATE_PROBLEM_CHOICE);

        // TODO: Validate files

        // Insert problem
        Problem problem = createProblemRequestDtoMapper.toProblem(problemCreateDto);
        problem.setServerId(authorizationContext.server().getId());
        problemMapper.insert(problem);

        if (problem.getId() == null) {
            throw new IllegalStateException("Problem insertion failed, no ID returned.");
        }
        log.info("Inserted problem with ID: {}", problem.getId());

        // Insert choices
        List<CreateProblemChoiceRequestDto> choices = problemCreateDto.getChoices();
        if (choices != null) {
            for (CreateProblemChoiceRequestDto choiceDto : choices) {
                ProblemChoice problemChoice = createProblemChoiceRequestDtoMapper.toProblemChoice(choiceDto);
                problemChoice.setProblemId(problem.getId());
                problemChoiceMapper.insert(problemChoice);
                log.info("Inserted problem choice for problemId: {}", problem.getId());
            }
        }
    }

    public void validateProblem(String question, String operation) {
        if (question == null || question.trim().isEmpty()) {
            throw new ValidationException(operation, "question", "must not be empty");
        }
        // TODO Validate duplicates
        log.info("Validated problem creation DTO with question: '{}'", question.trim());
    }

}