package com.quip.backend.problem.dto.response;

import lombok.*;

import java.util.List;

/**
 * Data Transfer Object for returning detailed problem information.
 * <p>
 * This DTO contains comprehensive information about a problem to be returned
 * to the client, including its question text, choices, and media URL.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemResponseDto {
    /**
     * The text of the question or problem.
     */
    private String question;
    
    /**
     * List of choices available for this problem.
     */
    private List<GetProblemChoiceResponseDto> choices;
    
    /**
     * URL to the media file associated with this problem, if any.
     */
    private String mediaUrl;
}