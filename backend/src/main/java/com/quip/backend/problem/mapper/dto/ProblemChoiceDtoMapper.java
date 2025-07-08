package com.quip.backend.problem.mapper.dto;

import com.quip.backend.problem.dto.ProblemChoiceCreateDto;
import com.quip.backend.problem.dto.ProblemChoiceDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.model.ProblemChoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface ProblemChoiceDtoMapper {

    @Mappings({
            @Mapping(target = "mediaUrl", ignore = true)
    })
    ProblemChoiceDto toProblemChoiceDto(ProblemChoice problemChoice);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true),
            @Mapping(target = "problemId", ignore = true) // will be set explicitly in service
    })
    ProblemChoice toProblemChoice(ProblemChoiceCreateDto dto);
}