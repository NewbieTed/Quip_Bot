package com.quip.backend.problem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for returning problem category information.
 * <p>
 * This DTO contains the essential information about a problem category
 * to be returned to the client, including its ID, name, and description.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemCategoryResponseDto {
    /**
     * Unique identifier of the problem category.
     */
    private Long problemCategoryId;
    
    /**
     * Name of the problem category.
     */
    private String categoryName;
    
    /**
     * Detailed description of the problem category.
     */
    private String description;
}
