package com.quip.backend.problem.mapper.dto;

import com.quip.backend.problem.dto.ProblemChoiceDto;
import com.quip.backend.problem.dto.ProblemCreateDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.model.Problem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProblemCreateDtoMapper {

    @Mappings({
            @Mapping(target = "mediaUrl", ignore = true)
    })
    ProblemDto toProblemDto(Problem problem, List<ProblemChoiceDto> choices);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "mediaFileId", ignore = true),
            @Mapping(target = "isValid", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true)
    })
    Problem toProblem(ProblemCreateDto dto);
}