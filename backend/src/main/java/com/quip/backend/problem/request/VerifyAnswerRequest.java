package com.quip.backend.problem.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyAnswerRequest {

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    @NotNull(message = "Answer cannot be null")
    @NotBlank(message = "Answer cannot be blank")
    private String answer;
}