package com.quip.backend.problem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for returning problem list item information.
 * <p>
 * This DTO contains the essential information about a problem to be displayed
 * in a list view, currently just the question text. It can be expanded in the
 * future to include more fields as needed.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemListItemResponseDto {
    private String question;
}
