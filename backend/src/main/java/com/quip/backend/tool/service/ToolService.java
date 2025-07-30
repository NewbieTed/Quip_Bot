package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.mapper.dto.response.ToolResponseDtoMapper;
import com.quip.backend.tool.model.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Service responsible for managing tools in the system.
 * <p>
 * This service handles the registration, retrieval, and management of tools.
 * It provides functionality for registering new tools from MCP servers,
 * managing tool availability, and retrieving tools based on various criteria.
 * </p>
 * <p>
 * The service works with both built-in tools and MCP server tools,
 * providing a unified interface for tool management.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolService {

    // Service dependencies
    private final AuthorizationService authorizationService;
    private final McpServerService mcpServerService;

    // Database mappers
    private final ToolMapper toolMapper;

    // DTO mappers
    private final ToolResponseDtoMapper toolResponseDtoMapper;

    private static final String REGISTER_TOOL = "Tool Registration";
    private static final String UPDATE_TOOL = "Tool Update";
    private static final String DELETE_TOOL = "Tool Deletion";
    private static final String RETRIEVE_TOOL = "Tool Retrieval";
    private static final String MANAGE_TOOL = "Tool Management";
    private static final String ENABLE_TOOL = "Tool Enable";
    private static final String DISABLE_TOOL = "Tool Disable";

    /**
     * Retrieves tools by their IDs using batch selection for optimal performance.
     * <p>
     * This method uses MyBatis Plus selectBatchIds to efficiently fetch multiple tools
     * in a single database query. It's commonly used when you have a collection of
     * tool IDs and need to get the corresponding tool details.
     * </p>
     *
     * @param toolIds Collection of tool IDs to retrieve
     * @return List of Tool entities matching the provided IDs
     */
    public List<Tool> getToolsByIds(Collection<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            log.debug("No tool IDs provided, returning empty list");
            return List.of();
        }

        log.debug("Retrieving {} tools by IDs", toolIds.size());
        List<Tool> tools = toolMapper.selectBatchIds(toolIds);
        log.debug("Found {} tools for {} requested IDs", tools.size(), toolIds.size());
        
        return tools;
    }

    /**
     * Retrieves a tool by its name.
     * <p>
     * This method finds a tool by its unique tool name. Since tool names are guaranteed
     * to be unique, this method returns a single Tool entity or null if not found.
     * </p>
     *
     * @param toolName The name of the tool to retrieve
     * @return Tool entity matching the provided name, or null if not found
     */
    public Tool getToolByName(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            log.debug("Tool name is null or empty, returning null");
            return null;
        }

        log.debug("Retrieving tool by name: {}", toolName);
        Tool tool = toolMapper.selectOne(new QueryWrapper<Tool>()
                .eq("tool_name", toolName));
        
        if (tool != null) {
            log.debug("Found tool with ID {} for name: {}", tool.getId(), toolName);
        } else {
            log.debug("No tool found for name: {}", toolName);
        }
        
        return tool;
    }

    /**
     * Validates that a tool exists for a given operation.
     * <p>
     * This method checks if the provided tool name is valid and refers to an existing tool.
     * It's used during operations that require a valid tool reference.
     * </p>
     *
     * @param toolName The name of the tool to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return The validated Tool entity
     * @throws ValidationException If the tool name is null/empty or refers to a non-existent tool
     * @throws IllegalArgumentException If the operation parameter is null
     */
    public Tool validateTool(String toolName, String operation) {
        // Validate required parameters
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new ValidationException(operation, "toolName", "must not be null or empty");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        
        // Check if tool exists in the database
        Tool tool = getToolByName(toolName);
        if (tool == null) {
            throw new ValidationException(operation, "toolName", "must refer to an existing tool");
        }
        
        log.info("Validated tool: {} (ID: {})", toolName, tool.getId());
        return tool;
    }

}