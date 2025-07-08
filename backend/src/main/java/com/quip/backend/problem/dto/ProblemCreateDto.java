package com.quip.backend.problem.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemCreateDto {
    @NotBlank(message = "Question cannot be blank")
    private String question;

    @NotEmpty(message = "Choices cannot be empty")
    private List<@NotNull(message = "Each choice cannot be null") ProblemChoiceCreateDto> choices;

    private Long mediaFileId;

    @NotNull(message = "Contributor ID cannot be null")
    @PositiveOrZero(message = "Contributor ID must be a non negative number")
    private Long contributorId;
}