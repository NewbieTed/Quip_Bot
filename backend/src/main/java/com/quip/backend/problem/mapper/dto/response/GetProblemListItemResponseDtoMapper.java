package com.quip.backend.problem.mapper.dto.response;

import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.model.Problem;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface GetProblemListItemResponseDtoMapper {
    @Mappings({
    })
    GetProblemListItemResponseDto toGetProblemListResponseDto(Problem problem);
}
