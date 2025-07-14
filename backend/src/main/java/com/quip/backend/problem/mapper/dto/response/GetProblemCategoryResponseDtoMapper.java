package com.quip.backend.problem.mapper.dto.response;


import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.model.ProblemCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface GetProblemCategoryResponseDtoMapper {
    @Mappings({
            @Mapping(target = "problemCategoryId", source = "id")
    })
    GetProblemCategoryResponseDto toProblemCategoryDto(ProblemCategory dto);
}
