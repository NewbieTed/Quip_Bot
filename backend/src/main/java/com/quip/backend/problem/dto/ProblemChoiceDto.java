package com.quip.backend.problem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemChoiceDto {
    /**
     * Choice text, nullable if mediaFileId is provided.
     */
    private String choiceText;

    /**
     * Media file ID, nullable if choiceText is provided.
     */
    private String mediaUrl;


    private Boolean isCorrect;
}
