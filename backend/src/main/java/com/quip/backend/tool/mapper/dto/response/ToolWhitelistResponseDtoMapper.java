package com.quip.backend.tool.mapper.dto.response;

import com.quip.backend.tool.dto.response.ToolWhitelistResponseDto;
import com.quip.backend.tool.model.ToolWhitelist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper interface for converting ToolWhitelist entity to ToolWhitelistResponseDto.
 * <p>
 * This interface defines mapping rules for transforming domain entities into response DTOs.
 * It uses MapStruct to automatically generate the implementation at compile time.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ToolWhitelistResponseDtoMapper {
    /**
     * Converts a ToolWhitelist entity to a ToolWhitelistResponseDto.
     * <p>
     * This mapping uses direct field mapping for most properties.
     * The following fields are ignored as they need to be populated separately
     * by joining with related entities:
     * - memberName (from Member entity)
     * - toolName (from Tool entity)
     * - serverName (from Server entity)
     * - isActive (calculated based on expiresAt)
     * </p>
     *
     * @param toolWhitelist The entity to convert
     * @return A response DTO with mapped values
     */
    @Mapping(target = "memberName", ignore = true)
    @Mapping(target = "toolName", ignore = true)
    @Mapping(target = "serverName", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    ToolWhitelistResponseDto toToolWhitelistResponseDto(ToolWhitelist toolWhitelist);
}