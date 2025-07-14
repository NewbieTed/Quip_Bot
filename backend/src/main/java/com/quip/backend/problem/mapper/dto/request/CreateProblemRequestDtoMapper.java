package com.quip.backend.problem.mapper.dto.request;

import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.model.Problem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CreateProblemRequestDtoMapper {


    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "mediaFileId", ignore = true),
            @Mapping(target = "isValid", ignore = true),
            @Mapping(target = "contributorId", source = "memberId"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true)
    })
    Problem toProblem(CreateProblemRequestDto dto);
}