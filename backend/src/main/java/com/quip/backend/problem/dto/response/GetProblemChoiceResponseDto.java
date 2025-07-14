package com.quip.backend.problem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemChoiceResponseDto {
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
