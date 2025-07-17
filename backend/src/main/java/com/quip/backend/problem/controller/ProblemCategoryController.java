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

@RestController
@RequestMapping("/problem-categories")
@RequiredArgsConstructor
@Validated
public class ProblemCategoryController {
    // Service modules
    private final ProblemCategoryService problemCategoryService;
    private final Validator validator;

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

    @PostMapping("/create")
    public BaseResponse<Boolean> addProblemCategory(@Valid @RequestBody CreateProblemCategoryRequestDto createProblemCategoryRequestDto) {
        problemCategoryService.addProblemCategory(createProblemCategoryRequestDto);
        return BaseResponse.success();
    }
}
