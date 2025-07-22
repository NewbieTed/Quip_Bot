package com.quip.backend.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating a new problem category.
 * <p>
 * This DTO contains all the information needed to create a new problem category,
 * including the category name, description, and metadata about the request context
 * (channel, member) for authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProblemCategoryRequestDto {
    /**
     * ID of the channel where this category is being created.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the member creating this category.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * Name of the problem category.
     * Must not be blank.
     */
    @NotNull(message = "Problem Category Name cannot be null")
    @NotBlank(message = "Problem Category Name not be an empty string")
    private String problemCategoryName;

    /**
     * Description of the problem category.
     * Must not be blank.
     */
    @NotNull(message = "Problem Category Description cannot be null")
    @NotBlank(message = "Problem Category Description not be an empty string")
    private String problemCategoryDescription;
}
