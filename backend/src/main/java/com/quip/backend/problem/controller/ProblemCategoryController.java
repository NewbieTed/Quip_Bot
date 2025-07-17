package com.quip.backend.problem.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.service.ProblemCategoryService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing problem categories.
 * <p>
 * This controller provides endpoints for creating and retrieving problem categories.
 * It handles HTTP requests related to problem category management and delegates
 * business logic to the ProblemCategoryService.
 * </p>
 */
@RestController
@RequestMapping("/problem-categories")
@RequiredArgsConstructor
@Validated
public class ProblemCategoryController {
    /**
     * Service for problem category-related operations.
     */
    private final ProblemCategoryService problemCategoryService;
    
    /**
     * Validator for manual validation of request parameters.
     */
    private final Validator validator;

    /**
     * Retrieves all problem categories for a server.
     * <p>
     * This endpoint returns all problem categories available in the server associated
     * with the specified channel, provided the requesting member has permission to
     * view them.
     * </p>
     *
     * @param channelId The ID of the channel where the request is being made
     * @param memberId The ID of the member making the request
     * @return A response containing a list of problem category DTOs
     * @throws ConstraintViolationException If the request parameters fail validation
     */
    @GetMapping("/list")
    public BaseResponse<List<GetProblemCategoryResponseDto>> getByChannelId(
            @RequestParam("channelId") Long channelId,
            @RequestParam("memberId") Long memberId) {

        GetProblemCategoryRequestDto getProblemCategoryRequestDto = new GetProblemCategoryRequestDto(channelId, memberId);
        var violations = validator.validate(getProblemCategoryRequestDto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        return BaseResponse.success(problemCategoryService.getServerProblemCategories(getProblemCategoryRequestDto));
    }

    /**
     * Creates a new problem category.
     * <p>
     * This endpoint creates a new problem category with the provided details.
     * The requesting member must have permission to manage problem categories
     * in the specified channel.
     * </p>
     *
     * @param createProblemCategoryRequestDto The DTO containing the category details
     * @return A success response if the category was created successfully
     */
    @PostMapping("/create")
    public BaseResponse<Boolean> addProblemCategory(@Valid @RequestBody CreateProblemCategoryRequestDto createProblemCategoryRequestDto) {
        problemCategoryService.addProblemCategory(createProblemCategoryRequestDto);
        return BaseResponse.success();
    }
}
