package com.quip.backend.problem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for returning problem choice information.
 * <p>
 * This DTO contains the essential information about a problem choice
 * to be returned to the client, including its text content, media URL,
 * and whether it is the correct answer.
 * </p>
 */
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


    /**
     * Flag indicating whether this choice is the correct answer to the problem.
     */
    private Boolean isCorrect;
}
