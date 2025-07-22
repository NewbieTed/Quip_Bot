package com.quip.backend.problem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for retrieving problem categories.
 * <p>
 * This DTO contains the information needed to retrieve problem categories
 * for a server, including the request context (channel, member) for
 * authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemCategoryRequestDto {
    /**
     * ID of the channel where this request is being made.
     * Used for authorization checks and to determine the server.
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
}
