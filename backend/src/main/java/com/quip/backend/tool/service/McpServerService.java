package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.tool.mapper.database.McpServerMapper;
import com.quip.backend.tool.mapper.dto.response.McpServerResponseDtoMapper;
import com.quip.backend.tool.model.McpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing MCP servers in the system.
 * <p>
 * This service handles the creation, retrieval, updating, and deletion of MCP servers.
 * It provides functionality for registering new MCP servers, managing their lifecycle,
 * and discovering tools from registered servers.
 * </p>
 * <p>
 * The service enforces authorization rules to ensure that only authorized members
 * can perform operations on MCP servers.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    // Service dependencies
    private final AuthorizationService authorizationService;

    // Database mappers
    private final McpServerMapper mcpServerMapper;

    // DTO mappers
    private final McpServerResponseDtoMapper mcpServerResponseDtoMapper;

    private static final String CREATE_MCP_SERVER = "MCP Server Creation";
    private static final String UPDATE_MCP_SERVER = "MCP Server Update";
    private static final String DELETE_MCP_SERVER = "MCP Server Deletion";
    private static final String RETRIEVE_MCP_SERVER = "MCP Server Retrieval";
    private static final String MANAGE_MCP_SERVER = "MCP Server Management";

    /**
     * Finds an MCP server by its name.
     * 
     * @param serverName the name of the MCP server
     * @return the MCP server
     * @throws RuntimeException if server is not found
     */
    public McpServer findByServerName(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            throw new IllegalArgumentException("MCP server name cannot be null or empty");
        }
        
        // Query the database for MCP server by serverName
        QueryWrapper<McpServer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("server_name", serverName.trim());
        
        McpServer mcpServer = mcpServerMapper.selectOne(queryWrapper);
        
        if (mcpServer == null) {
            throw new RuntimeException("MCP server not found: " + serverName);
        }
        
        return mcpServer;
    }

}