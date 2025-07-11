package com.quip.backend.problem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a ProblemChoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemChoiceCreateDto {

    /**
     * Choice text, nullable if mediaFileId is provided.
     */
    @NotBlank(message = "Problem choice cannot be empty")
    private String choiceText;

    /**
     * Media file ID, nullable if choiceText is provided.
     */
    private Long mediaFileId;

    @NotNull(message = "Problem choice have to specify whether it is correct or not")
    private Boolean isCorrect;
}
