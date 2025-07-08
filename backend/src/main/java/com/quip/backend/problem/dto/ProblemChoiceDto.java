package com.quip.backend.problem.dto;

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
}
