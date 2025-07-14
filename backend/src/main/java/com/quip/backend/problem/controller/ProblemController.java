package com.quip.backend.problem.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final ProblemMapper problemMapper;

//    @GetMapping()
//    public BaseResponse<ProblemDto> getProblem() {
//        return BaseResponse.success("success", problemService.getProblem());
//    }
//


    @PostMapping("/create")
    public BaseResponse<Boolean> addProblem(@Valid @RequestBody CreateProblemRequestDto problemCreateDto) {
        problemService.addProblem(problemCreateDto);
        return BaseResponse.success();
    }
}