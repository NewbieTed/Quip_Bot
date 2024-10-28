package com.quip.backend.problem.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyAnswerRequest {

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    @NotNull(message = "Answer cannot be null")
    @NotBlank(message = "Answer cannot be blank")
    private String answer;
}
