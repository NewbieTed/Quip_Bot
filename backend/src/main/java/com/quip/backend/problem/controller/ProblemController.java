package com.quip.backend.problem.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.ProblemDTO;
import com.quip.backend.problem.request.VerifyAnswerRequest;
import com.quip.backend.problem.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {


    private final ProblemService problemService;

    @GetMapping()
    public BaseResponse<ProblemDTO> getProblem() {
        return problemService.getProblem();
    }

    @PostMapping("/verify")
    public BaseResponse<Boolean> verifyAnswer(@Valid @RequestBody VerifyAnswerRequest request) {
        if (request == null) {
            return BaseResponse.failure(HttpStatus.BAD_REQUEST.value(), "No data provided");
        }
        return problemService.verifyAnswer(request.getProblemId(), request.getAnswer());
    }


//    // Handle GET request to retrieve a user by ID
//    @GetMapping("/statistics")
//    public Problem getUserById(@PathVariable Long id) {
//        return problemService.getUserById(id);
//    }
//
//    // Handle POST request to create a new user
//    @PostMapping
//    public Problem createUser(@RequestBody Problem problem) {
//        return problemService.createUser(problem);
//    }
//
//    // Handle PUT request to update an existing user
//    @PutMapping("/{id}")
//    public Problem updateUser(@PathVariable Long id, @RequestBody Problem problem) {
//        return problemService.updateUser(id, problem);
//    }
//
//    // Handle DELETE request to delete a user
//    @DeleteMapping("/{id}")
//    public void deleteUser(@PathVariable Long id) {
//        problemService.deleteUser(id);
//    }
}
