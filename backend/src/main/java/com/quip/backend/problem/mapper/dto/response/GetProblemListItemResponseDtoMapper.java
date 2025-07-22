package com.quip.backend.problem.mapper.dto.response;

import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.model.Problem;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

/**
 * MapStruct mapper interface for converting Problem entity to GetProblemListItemResponseDto.
 * <p>
 * This interface defines mapping rules for transforming domain entities into response DTOs
 * for list views. It uses MapStruct to automatically generate the implementation at compile time.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface GetProblemListItemResponseDtoMapper {
    /**
     * Converts a Problem entity to a GetProblemListItemResponseDto.
     * <p>
     * This mapping automatically maps the question field from the Problem entity
     * to the question field in the DTO. No explicit mappings are needed since
     * the field names match.
     * </p>
     *
     * @param problem The entity to convert
     * @return A response DTO with mapped values
     */
    @Mappings({
        // No explicit mappings needed as field names match
    })
    GetProblemListItemResponseDto toGetProblemListResponseDto(Problem problem);
}
