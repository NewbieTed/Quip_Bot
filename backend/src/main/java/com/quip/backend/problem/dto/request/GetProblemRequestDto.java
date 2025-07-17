package com.quip.backend.problem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for retrieving problems by category.
 * <p>
 * This DTO contains the information needed to retrieve problems
 * belonging to a specific category, including the request context
 * (channel, member) for authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemRequestDto {
    /**
     * ID of the channel where this request is being made.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the member making this request.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * ID of the problem category to retrieve problems from.
     */
    @NotNull(message = "Problem Category ID cannot be null")
    @PositiveOrZero(message = "Problem Category ID must be a non negative number")
    private Long problemCategoryId;
}
