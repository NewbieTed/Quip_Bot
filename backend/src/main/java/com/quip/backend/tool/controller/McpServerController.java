package com.quip.backend.tool.controller;

import com.quip.backend.tool.service.McpServerService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing MCP servers.
 * <p>
 * This controller provides endpoints for creating, retrieving, updating, and deleting MCP servers.
 * It handles HTTP requests related to MCP server management and delegates
 * business logic to the McpServerService.
 * </p>
 * <p>
 * Endpoints include:
 * - Creating new MCP servers
 * - Retrieving MCP server information
 * - Updating existing MCP servers
 * - Deleting MCP servers
 * - Discovering tools from MCP servers
 * - Health checking MCP servers
 * </p>
 */
@Validated
@RestController
@RequestMapping("/mcp-servers")
@RequiredArgsConstructor
public class McpServerController {
    /**
     * Service for MCP server-related operations.
     */
    private final McpServerService mcpServerService;

}