package com.quip.backend.problem.mapper.dto.request;

import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import com.quip.backend.problem.model.ProblemChoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface CreateProblemChoiceRequestDtoMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true),
            @Mapping(target = "problemId", ignore = true) // will be set explicitly in service
    })
    ProblemChoice toProblemChoice(CreateProblemChoiceRequestDto dto);
}