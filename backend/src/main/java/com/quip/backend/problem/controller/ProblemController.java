package com.quip.backend.problem.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.dto.request.GetProblemRequestDto;
import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.service.ProblemService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing problems.
 * <p>
 * This controller provides endpoints for creating and retrieving problems.
 * It handles HTTP requests related to problem management and delegates
 * business logic to the ProblemService.
 * </p>
 */
@Validated
@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
public class ProblemController {
    /**
     * Service for problem-related operations.
     */
    private final ProblemService problemService;
    
    /**
     * Validator for manual validation of request parameters.
     */
    private final Validator validator;

//    @GetMapping()
//    public BaseResponse<ProblemDto> getProblem() {
//        return BaseResponse.success("success", problemService.getProblem());
//    }
//

    /**
     * Retrieves a list of problems belonging to a specific category.
     * <p>
     * This endpoint returns all problems that belong to the specified category,
     * provided the requesting member has permission to view them in the specified channel.
     * </p>
     *
     * @param channelId The ID of the channel where the request is being made
     * @param memberId The ID of the member making the request
     * @param problemCategoryId The ID of the problem category to retrieve problems from
     * @return A response containing a list of problem DTOs
     * @throws jakarta.validation.ConstraintViolationException If the request parameters fail validation
     */
    @GetMapping("/list")
    public BaseResponse<List<GetProblemListItemResponseDto>> getProblemsByCategory(
            @RequestParam("channelId") Long channelId,
            @RequestParam("memberId") Long memberId,
            @RequestParam("problemCategoryId") Long problemCategoryId) {

        GetProblemRequestDto dto = new GetProblemRequestDto(channelId, memberId, problemCategoryId);
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }

        return BaseResponse.success(problemService.getProblemsByCategory(dto));
    }

    /**
     * Creates a new problem.
     * <p>
     * This endpoint creates a new problem with the provided details,
     * including its associated choices. The requesting member must have
     * permission to manage problems in the specified channel.
     * </p>
     *
     * @param createProblemRequestDto The DTO containing the problem details
     * @return A success response if the problem was created successfully
     */
    @PostMapping("/create")
    public BaseResponse<Boolean> addProblem(@Valid @RequestBody CreateProblemRequestDto createProblemRequestDto) {
        problemService.addProblem(createProblemRequestDto);
        return BaseResponse.success();
    }
}