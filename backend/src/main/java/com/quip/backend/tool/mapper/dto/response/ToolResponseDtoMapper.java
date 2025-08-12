package com.quip.backend.tool.mapper.dto.response;

import com.quip.backend.tool.dto.response.ToolResponseDto;
import com.quip.backend.tool.model.Tool;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper interface for converting Tool entity to ToolResponseDto.
 * <p>
 * This interface defines mapping rules for transforming domain entities into response DTOs.
 * It uses MapStruct to automatically generate the implementation at compile time.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ToolResponseDtoMapper {
    /**
     * Converts a Tool entity to a ToolResponseDto.
     * <p>
     * This mapping uses direct field mapping for most properties.
     * The mcpServerName field is ignored as it needs to be populated separately
     * by joining with the McpServer entity.
     * </p>
     *
     * @param tool The entity to convert
     * @return A response DTO with mapped values
     */
    @Mapping(target = "mcpServerName", ignore = true)
    ToolResponseDto toToolResponseDto(Tool tool);
}