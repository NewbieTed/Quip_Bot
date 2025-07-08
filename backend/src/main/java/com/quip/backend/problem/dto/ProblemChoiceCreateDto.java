package com.quip.backend.problem.dto;

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
    private String choiceText;

    /**
     * Media file ID, nullable if choiceText is provided.
     */
    private Long mediaFileId;

    @NotNull
    private Boolean isCorrect;
}
