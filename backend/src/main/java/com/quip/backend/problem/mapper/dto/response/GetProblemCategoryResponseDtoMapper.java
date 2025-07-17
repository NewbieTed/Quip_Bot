package com.quip.backend.problem.mapper.dto.response;


import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.model.ProblemCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * MapStruct mapper interface for converting ProblemCategory entity to GetProblemCategoryResponseDto.
 * <p>
 * This interface defines mapping rules for transforming domain entities into response DTOs.
 * It uses MapStruct to automatically generate the implementation at compile time.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface GetProblemCategoryResponseDtoMapper {
    /**
     * Converts a ProblemCategory entity to a GetProblemCategoryResponseDto.
     * <p>
     * This mapping:
     * - Maps id to problemCategoryId
     * - Maps categoryName and description directly
     * </p>
     *
     * @param dto The entity to convert
     * @return A response DTO with mapped values
     */
    @Mappings({
            @Mapping(target = "problemCategoryId", source = "id")
    })
    GetProblemCategoryResponseDto toProblemCategoryDto(ProblemCategory dto);
}
