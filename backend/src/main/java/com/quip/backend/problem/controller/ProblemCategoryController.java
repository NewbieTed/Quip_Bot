package com.quip.backend.problem.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.service.ProblemCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/problem-category")
@RequiredArgsConstructor
public class ProblemCategoryController {
    // Service modules
    private ProblemCategoryService problemCategoryService;

    @GetMapping("/list")
    public BaseResponse<List<GetProblemCategoryResponseDto>> getBySeverId(@Valid @RequestBody GetProblemCategoryRequestDto getProblemCategoryRequestDto) {
        return BaseResponse.success(problemCategoryService.getServerProblemCategories(getProblemCategoryRequestDto));
    }
}
