package com.quip.backend.tool.service;

import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.mapper.dto.response.ToolResponseDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // Authorization operation constants
    private static final String REGISTER_TOOL = "Tool Registration";
    private static final String UPDATE_TOOL = "Tool Update";
    private static final String DELETE_TOOL = "Tool Deletion";
    private static final String VIEW_TOOL = "Tool Retrieval";
    private static final String MANAGE_TOOL = "Tool Management";
    private static final String ENABLE_TOOL = "Tool Enable";
    private static final String DISABLE_TOOL = "Tool Disable";

}