package com.quip.backend.tool.mapper.dto.response;

import com.quip.backend.tool.dto.response.McpServerResponseDto;
import com.quip.backend.tool.model.McpServer;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper interface for converting McpServer entity to McpServerResponseDto.
 * <p>
 * This interface defines mapping rules for transforming domain entities into response DTOs.
 * It uses MapStruct to automatically generate the implementation at compile time.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface McpServerResponseDtoMapper {
    /**
     * Converts an McpServer entity to an McpServerResponseDto.
     * <p>
     * This mapping uses direct field mapping for all properties as they have the same names.
     * </p>
     *
     * @param mcpServer The entity to convert
     * @return A response DTO with mapped values
     */
    McpServerResponseDto toMcpServerResponseDto(McpServer mcpServer);
}