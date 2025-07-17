package com.quip.backend.problem.mapper.dto.request;

import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.model.ProblemCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CreateProblemCategoryRequestDtoMapper {
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "serverId", ignore = true),
            @Mapping(target = "categoryName", source = "problemCategoryName"),
            @Mapping(target = "description", source = "problemCategoryDescription"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true)
    })
    ProblemCategory toProblemCategory(CreateProblemCategoryRequestDto createProblemCategoryRequestDto);
}
