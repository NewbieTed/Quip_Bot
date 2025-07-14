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

@Validated
@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
public class ProblemController {
    private final ProblemService problemService;
    private final Validator validator;

//    @GetMapping()
//    public BaseResponse<ProblemDto> getProblem() {
//        return BaseResponse.success("success", problemService.getProblem());
//    }
//
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


    @PostMapping("/create")
    public BaseResponse<Boolean> addProblem(@Valid @RequestBody CreateProblemRequestDto createProblemRequestDto) {
        problemService.addProblem(createProblemRequestDto);
        return BaseResponse.success();
    }
}