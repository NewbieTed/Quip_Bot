package com.quip.backend.problem.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.ProblemCreateDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.request.VerifyAnswerRequest;
import com.quip.backend.problem.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final ProblemMapper problemMapper;

//    @GetMapping()
//    public BaseResponse<ProblemDto> getProblem() {
//        return BaseResponse.success("success", problemService.getProblem());
//    }
//
//    @GetMapping("/verify")
//    public BaseResponse<Boolean> verifyAnswer(@Valid @RequestBody VerifyAnswerRequest request) {
//        if (request == null) {
//            return BaseResponse.failure(HttpStatus.BAD_REQUEST.value(), "No data provided");
//        }
//
//        long problemId = request.getProblemId();
//        Problem problem = problemMapper.selectProblemById(problemId);
//
//        if (problem == null) {
//            return BaseResponse.failure(HttpStatus.NOT_FOUND.value(),
//                    "Problem with problem id " + problemId + " is not found");
//        }
//        return BaseResponse.success(problemService.verifyAnswer(problem, request.getAnswer()));
//    }

    @PostMapping("/create")
    public BaseResponse<Boolean> addProblem(@Valid @RequestBody ProblemCreateDto problemCreateDto) {
        problemService.addProblem(problemCreateDto);
        return BaseResponse.success();
    }
}