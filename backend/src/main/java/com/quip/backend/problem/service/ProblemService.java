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

/**
 * Service responsible for managing problems in the system.
 * <p>
 * This service handles the creation, retrieval, and management of problems.
 * It provides functionality for adding new problems with their associated choices,
 * retrieving problems by category, and validating problem data.
 * </p>
 * <p>
 * The service enforces authorization rules to ensure that only authorized members
 * can perform operations on problems.
 * </p>
 */
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

    /**
     * Gets a specific problem by ID.
     * <p>
     * This is a placeholder method for future implementation.
     * </p>
     *
     * @return Problem response DTO
     */
    public GetProblemResponseDto getProblem() {
        return null; // Placeholder for future implementation
    }

    // TODO: Change other feature's file structure on DTOs as well as naming

    /**
     * Retrieves a list of problems belonging to a specific category.
     * <p>
     * This method first validates that the requesting member has permission to view problems
     * in the specified channel, then retrieves all problems associated with the given category.
     * </p>
     *
     * @param getProblemRequestDto DTO containing member ID, channel ID, and problem category ID
     * @return List of problem response DTOs
     * @throws ValidationException If the member lacks proper authorization or if the category doesn't exist
     */
    public List<GetProblemListItemResponseDto> getProblemsByCategory(GetProblemRequestDto getProblemRequestDto) {
        // Verify member has permission to view problems in this channel
        authorizationService.validateAuthorization(
                getProblemRequestDto.getMemberId(),
                getProblemRequestDto.getChannelId(),
                AuthorizationConstants.VIEW_PROBLEM,
                RETRIEVE_PROBLEM
        );
        
        // Validate that the requested problem category exists
        ProblemCategory problemCategory = problemCategoryService.validateProblemCategory(getProblemRequestDto.getProblemCategoryId(), RETRIEVE_PROBLEM);

        // Retrieve all problems for the category and convert to DTOs
        List<Problem> problems = problemMapper.selectByProblemCategoryId(problemCategory.getId());
        List<GetProblemListItemResponseDto> getProblemResponseDtos = new ArrayList<>();

        for (Problem problem : problems) {
            getProblemResponseDtos.add(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(problem));
        }

        return getProblemResponseDtos;
    }

    /**
     * Adds a new problem to the system with its associated choices.
     * <p>
     * This method creates a new problem in the database along with any associated choices.
     * It performs validation on the input data and ensures that the requesting member
     * has proper authorization to manage problems in the specified channel.
     * </p>
     * <p>
     * The method is transactional to ensure that both the problem and its choices
     * are either all created successfully or none are created.
     * </p>
     *
     * @param problemCreateDto DTO containing the problem data to be created
     * @throws ValidationException If the input data is invalid or the member lacks proper authorization
     * @throws IllegalStateException If the problem insertion fails to return an ID
     */
    @Transactional
    public void addProblem(CreateProblemRequestDto problemCreateDto) {
        if (problemCreateDto == null) {
            throw new ValidationException(CREATE_PROBLEM, "body", "must not be null");
        }

        // Verify member has permission to manage problems in this channel
        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                problemCreateDto.getMemberId(),
                problemCreateDto.getChannelId(),
                AuthorizationConstants.MANAGE_PROBLEM,
                CREATE_PROBLEM
        );

        // Validate all required fields and relationships
        problemCategoryService.validateProblemCategory(problemCreateDto.getProblemCategoryId(), CREATE_PROBLEM);
        this.validateProblem(problemCreateDto.getQuestion(), CREATE_PROBLEM);
        problemChoiceService.validateProblemChoices(problemCreateDto.getChoices(), CREATE_PROBLEM_CHOICE);

        // TODO: Validate files

        // Create and persist the problem entity
        Problem problem = createProblemRequestDtoMapper.toProblem(problemCreateDto);
        problem.setServerId(authorizationContext.server().getId());
        problemMapper.insert(problem);

        if (problem.getId() == null) {
            throw new IllegalStateException("Problem insertion failed, no ID returned.");
        }
        log.info("Inserted problem with ID: {}", problem.getId());

        // Create and persist all associated problem choices
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

    /**
     * Validates a problem question for creation or update operations.
     * <p>
     * This method ensures that the question text is not null or empty.
     * Future implementations may include duplicate checking.
     * </p>
     *
     * @param question The problem question text to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @throws ValidationException If the question is null or empty
     */
    public void validateProblem(String question, String operation) {
        // Ensure question has actual content
        if (question == null || question.trim().isEmpty()) {
            throw new ValidationException(operation, "question", "must not be empty");
        }
        // TODO Validate duplicates
        log.info("Validated problem creation DTO with question: '{}'", question.trim());
    }

}