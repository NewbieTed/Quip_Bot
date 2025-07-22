package com.quip.backend.problem.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for creating a new problem.
 * <p>
 * This DTO contains all the information needed to create a new problem,
 * including the question text, choices, optional media file, and metadata
 * about the request context (channel, category, member).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProblemRequestDto {
    /**
     * The text of the question or problem.
     * Cannot be blank.
     */
    @NotBlank(message = "Question cannot be blank")
    private String question;

    /**
     * List of choices for this problem.
     * Must contain at least one choice.
     */
    @Valid
    @NotEmpty(message = "Choices cannot be empty")
    private List<@NotNull(message = "Each choice cannot be null") CreateProblemChoiceRequestDto> choices;

    /**
     * Optional ID of a media file associated with this problem.
     */
    private Long mediaFileId;

    /**
     * ID of the channel where this problem is being created.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the category this problem belongs to.
     */
    @NotNull(message = "Problem category ID cannot be null")
    @PositiveOrZero(message = "Problem category ID must be a non negative number")
    private Long problemCategoryId;

    /**
     * ID of the member creating this problem.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;
}