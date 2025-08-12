package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.mapper.dto.response.ToolResponseDtoMapper;
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.model.ToolInfo;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ToolSyncMetricsService metricsService;

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

    // TEMPORARY: Force failure for testing resync feature
    @Value("${app.tool-sync.test.force-failure:false}")
    private boolean forceToolSyncFailure;

    /**
     * Creates or updates a tool record based on agent discovery.
     * <p>
     * This method handles tool additions from the agent. If the tool already exists,
     * it will be enabled and updated. If it doesn't exist, a new tool record will be created.
     * This method is specifically designed for agent-driven tool updates via Redis.
     * </p>
     *
     * @param toolName The name of the tool to create or update
     * @throws ValidationException If the tool name is null or empty
     * @throws RuntimeException If database operations fail
     */
    public void createOrUpdateToolFromAgent(String toolName, String mcpServerName) {
        // TEMPORARY: Force failure for testing resync feature
        if (forceToolSyncFailure) {
            log.error("TESTING: Forcing tool sync failure for tool: {}", toolName);
            throw new RuntimeException("TESTING: Forced tool sync failure for resync testing");
        }
        long startTime = System.currentTimeMillis();
        
        // Validate input parameters
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new ValidationException(REGISTER_TOOL, "toolName", "must not be null or empty");
        }
        if (mcpServerName == null || mcpServerName.trim().isEmpty()) {
            throw new ValidationException(REGISTER_TOOL, "mcpServerName", "must not be null or empty");
        }

        // Structured logging for tool database operations
        log.info("Processing tool addition from agent - toolName={}, mcpServerName={}, operation=create_or_update", 
                toolName, mcpServerName);

        try {
            // Resolve MCP server ID from server name
            Long mcpServerId = resolveMcpServerId(mcpServerName);
            
            // Check if tool already exists
            Tool existingTool = getToolByName(toolName);
            
            if (existingTool != null) {
                // Tool exists - check if it's already enabled to avoid unnecessary DB operations
                if (Boolean.TRUE.equals(existingTool.getEnabled())) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.info("Tool from agent already exists and is enabled - toolName={}, toolId={}, operation=no_change, processingTime={}ms",
                            toolName, existingTool.getId(), processingTime);
                    return; // Early return to avoid unnecessary processing
                }
                
                // Tool exists but is disabled - enable it
                existingTool.setEnabled(true);
                existingTool.setUpdatedAt(java.time.OffsetDateTime.now());
                
                int updatedRows = toolMapper.updateById(existingTool);
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Record database operation metrics
                metricsService.recordToolDatabaseOperation("enable", updatedRows > 0, processingTime);
                
                if (updatedRows > 0) {
                    // Log tool database operations (Requirement 4.2)
                    log.info("Successfully enabled existing tool from agent - toolName={}, toolId={}, operation=enable, processingTime={}ms", 
                            toolName, existingTool.getId(), processingTime);
                } else {
                    log.warn("Failed to update existing tool from agent - toolName={}, toolId={}, operation=enable, processingTime={}ms", 
                            toolName, existingTool.getId(), processingTime);
                    throw new RuntimeException("Failed to update tool in database: " + toolName);
                }
            } else {
                // Tool doesn't exist - create new record
                Tool newTool = Tool.builder()
                    .toolName(toolName)
                    .description("Tool discovered by agent")
                    .enabled(true)
                    .mcpServerId(mcpServerId)
                    .createdBy(null) // Agent-created tools don't have a specific user
                    .updatedBy(null)
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();

                int insertedRows = toolMapper.insert(newTool);
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Record database operation metrics
                metricsService.recordToolDatabaseOperation("create", insertedRows > 0, processingTime);
                
                if (insertedRows > 0) {
                    // Log tool database operations (Requirement 4.2)
                    log.info("Successfully created new tool from agent - toolName={}, toolId={}, operation=create, processingTime={}ms", 
                            toolName, newTool.getId(), processingTime);
                } else {
                    // Error logging with context information (Requirement 4.3)
                    log.error("Failed to create new tool from agent - toolName={}, operation=create, processingTime={}ms, insertedRows={}", 
                            toolName, processingTime, insertedRows);
                    throw new RuntimeException("Failed to create tool in database: " + toolName);
                }
            }
        } catch (ValidationException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            // Error logging with context information (Requirement 4.3)
            log.error("Validation error while processing tool addition from agent - toolName={}, operation=create_or_update, processingTime={}ms, errorType=ValidationException", 
                    toolName, processingTime, e);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            // Error logging with context information (Requirement 4.3)
            log.error("Unexpected error while processing tool addition from agent - toolName={}, operation=create_or_update, processingTime={}ms, errorType={}", 
                    toolName, processingTime, e.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to process tool addition from agent: " + toolName, e);
        }
    }

    /**
     * Disables a tool record based on agent discovery.
     * <p>
     * This method handles tool removals from the agent. It marks the tool as disabled
     * rather than deleting it to maintain audit trail and allow for potential re-enabling.
     * This method is specifically designed for agent-driven tool updates via Redis.
     * </p>
     *
     * @param toolName The name of the tool to disable
     * @throws ValidationException If the tool name is null or empty
     * @throws RuntimeException If database operations fail
     */
    public void disableToolFromAgent(String toolName) {
        long startTime = System.currentTimeMillis();
        
        // Validate input parameters
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new ValidationException(DISABLE_TOOL, "toolName", "must not be null or empty");
        }

        // Structured logging for tool database operations (Requirement 4.2)
        log.info("Processing tool removal from agent - toolName={}, operation=disable", toolName);

        try {
            // Check if tool exists
            Tool existingTool = getToolByName(toolName);
            
            if (existingTool != null) {
                // Tool exists - check if it's already disabled to avoid unnecessary DB operations
                if (!Boolean.TRUE.equals(existingTool.getEnabled())) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.debug("Tool from agent is already disabled - toolName={}, toolId={}, operation=no_change, processingTime={}ms", 
                            toolName, existingTool.getId(), processingTime);
                    return; // Early return to avoid unnecessary processing
                }
                
                // Tool exists and is enabled - disable it
                existingTool.setEnabled(false);
                existingTool.setUpdatedAt(java.time.OffsetDateTime.now());
                
                int updatedRows = toolMapper.updateById(existingTool);
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Record database operation metrics
                metricsService.recordToolDatabaseOperation("disable", updatedRows > 0, processingTime);
                
                if (updatedRows > 0) {
                    // Log tool database operations (Requirement 4.2)
                    log.info("Successfully disabled tool from agent - toolName={}, toolId={}, operation=disable, processingTime={}ms", 
                            toolName, existingTool.getId(), processingTime);
                } else {
                    log.warn("Failed to disable tool from agent - toolName={}, toolId={}, operation=disable, processingTime={}ms, updatedRows={}", 
                            toolName, existingTool.getId(), processingTime, updatedRows);
                    throw new RuntimeException("Failed to disable tool in database: " + toolName);
                }
            } else {
                long processingTime = System.currentTimeMillis() - startTime;
                // Tool doesn't exist - log warning but don't fail
                log.warn("Attempted to disable non-existent tool from agent - toolName={}, operation=disable, processingTime={}ms, result=not_found", 
                        toolName, processingTime);
            }
        } catch (ValidationException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            // Error logging with context information (Requirement 4.3)
            log.error("Validation error while processing tool removal from agent - toolName={}, operation=disable, processingTime={}ms, errorType=ValidationException", 
                    toolName, processingTime, e);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            // Error logging with context information (Requirement 4.3)
            log.error("Unexpected error while processing tool removal from agent - toolName={}, operation=disable, processingTime={}ms, errorType={}", 
                    toolName, processingTime, e.getClass().getSimpleName(), e);
            throw new RuntimeException("Failed to process tool removal from agent: " + toolName, e);
        }
    }

    /**
     * Retrieves all tool names from the database.
     * <p>
     * This method is used to get the current state of tools in the database
     * for comparison with agent tool inventory during sync operations.
     * </p>
     *
     * @return List of all tool names currently in the database
     */
    public List<String> getAllToolNames() {
        log.debug("Retrieving all tool names from database");
        
        List<Tool> allTools = toolMapper.selectList(new QueryWrapper<>());
        List<String> toolNames = allTools.stream()
                .map(Tool::getToolName)
                .collect(Collectors.toList());
        
        log.debug("Found {} tool names in database", toolNames.size());
        return toolNames;
    }

    /**
     * Synchronizes the database tool inventory with the complete tool list from the agent.
     * <p>
     * This method performs a full inventory sync by comparing the agent's current tool list
     * with the database state and making the necessary changes to align them. It calculates
     * differences between agent and database tools, then performs batch operations to
     * enable/disable tools as needed. All operations are performed within a transaction
     * to ensure consistency.
     * </p>
     * <p>
     * This method is specifically designed for sync recovery operations where the backend
     * needs to align its tool state with the agent's current reality.
     * </p>
     *
     * @param currentToolInfos List of ToolInfo objects currently available in the agent
     * @throws ValidationException If the currentToolInfos list is null
     * @throws RuntimeException If database operations fail
     */
    @Transactional
    public void syncToolsFromInventoryWithServerInfo(List<ToolInfo> currentToolInfos) {
        long startTime = System.currentTimeMillis();
        
        // Validate input parameters
        if (currentToolInfos == null) {
            throw new ValidationException("Tool Inventory Sync", "currentToolInfos", "must not be null");
        }

        // Comprehensive logging for sync operations (Requirement 6.4)
        log.info("Starting full tool inventory sync - agentToolCount={}, operation=inventory_sync", currentToolInfos.size());

        try {
            // Get current database state
            List<String> databaseTools = getAllToolNames();
            
            // Calculate differences between agent and database tools
            Set<String> agentToolSet = currentToolInfos.stream()
                    .filter(toolInfo -> toolInfo != null && toolInfo.getName() != null && !toolInfo.getName().trim().isEmpty())
                    .map(ToolInfo::getName)
                    .collect(Collectors.toSet());
            Set<String> databaseToolSet = new HashSet<>(databaseTools);

            // Tools that exist in agent but not in database (need to be created/enabled)
            List<ToolInfo> toolsToAdd = currentToolInfos.stream()
                    .filter(toolInfo -> toolInfo != null && toolInfo.getName() != null && !databaseToolSet.contains(toolInfo.getName()))
                    .toList();

            // Tools that exist in database but not in agent (need to be disabled)
            Set<String> toolsToDisable = databaseToolSet.stream()
                    .filter(tool -> !agentToolSet.contains(tool))
                    .collect(Collectors.toSet());

            // Tools that exist in both (need to be enabled if currently disabled)
            List<ToolInfo> toolsToEnable = currentToolInfos.stream()
                    .filter(toolInfo -> toolInfo != null && toolInfo.getName() != null && databaseToolSet.contains(toolInfo.getName()))
                    .toList();

            // Comprehensive logging for sync operations
            log.info("Calculated tool inventory differences - toolsToAdd={}, toolsToDisable={}, toolsToEnable={}, operation=calculate_differences", 
                    toolsToAdd.size(), toolsToDisable.size(), toolsToEnable.size());

            // Batch processing for enabling/disabling multiple tools
            int totalOperations = 0;
            int successfulOperations = 0;

            // Process tools to add (create new tools)
            if (!toolsToAdd.isEmpty()) {
                log.info("Processing tools to add - count={}, operation=batch_add", toolsToAdd.size());
                for (ToolInfo toolInfo : toolsToAdd) {
                    try {
                        String toolName = toolInfo.getName();
                        String mcpServerName = toolInfo.getMcpServerName();
                        createOrUpdateToolFromAgent(toolName, mcpServerName);
                        successfulOperations++;
                    } catch (Exception e) {
                        String toolName = toolInfo != null ? toolInfo.getName() : "unknown";
                        String mcpServerName = toolInfo != null ? toolInfo.getMcpServerName() : "unknown";
                        log.error("Failed to add tool during inventory sync - toolName={}, mcpServerName={}, operation=batch_add, error={}", 
                                toolName, mcpServerName, e.getMessage(), e);
                    }
                    totalOperations++;
                }
            }

            // Process tools to disable (mark as disabled)
            if (!toolsToDisable.isEmpty()) {
                log.info("Processing tools to disable - count={}, operation=batch_disable", toolsToDisable.size());
                for (String toolName : toolsToDisable) {
                    try {
                        disableToolFromAgent(toolName);
                        successfulOperations++;
                    } catch (Exception e) {
                        log.error("Failed to disable tool during inventory sync - toolName={}, operation=batch_disable, error={}", 
                                toolName, e.getMessage(), e);
                    }
                    totalOperations++;
                }
            }

            // Process tools to enable (ensure they are enabled)
            if (!toolsToEnable.isEmpty()) {
                log.info("Processing tools to enable - count={}, operation=batch_enable", toolsToEnable.size());
                for (ToolInfo toolInfo : toolsToEnable) {
                    try {
                        String toolName = toolInfo.getName();
                        String mcpServerName = toolInfo.getMcpServerName();
                        
                        // Get the tool and ensure it's enabled
                        Tool existingTool = getToolByName(toolName);
                        if (existingTool != null && !Boolean.TRUE.equals(existingTool.getEnabled())) {
                            existingTool.setEnabled(true);
                            existingTool.setUpdatedAt(java.time.OffsetDateTime.now());
                            
                            int updatedRows = toolMapper.updateById(existingTool);
                            if (updatedRows > 0) {
                                log.debug("Enabled tool during inventory sync - toolName={}, mcpServerName={}, toolId={}, operation=batch_enable", 
                                        toolName, mcpServerName, existingTool.getId());
                                successfulOperations++;
                            } else {
                                log.warn("Failed to enable tool during inventory sync - toolName={}, mcpServerName={}, toolId={}, operation=batch_enable", 
                                        toolName, mcpServerName, existingTool.getId());
                            }
                        } else if (existingTool != null) {
                            // Tool is already enabled, count as successful
                            successfulOperations++;
                        }
                    } catch (Exception e) {
                        String toolName = toolInfo != null ? toolInfo.getName() : "unknown";
                        String mcpServerName = toolInfo != null ? toolInfo.getMcpServerName() : "unknown";
                        log.error("Failed to enable tool during inventory sync - toolName={}, mcpServerName={}, operation=batch_enable, error={}", 
                                toolName, mcpServerName, e.getMessage(), e);
                    }
                    totalOperations++;
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record comprehensive metrics for the sync operation
            metricsService.recordToolDatabaseOperation("inventory_sync", successfulOperations == totalOperations, processingTime);

            // Comprehensive logging for sync operations (Requirement 6.4)
            log.info("Completed full tool inventory sync - totalOperations={}, successfulOperations={}, failedOperations={}, " +
                    "processingTime={}ms, operation=inventory_sync, success={}", 
                    totalOperations, successfulOperations, (totalOperations - successfulOperations), 
                    processingTime, (successfulOperations == totalOperations));

            // If not all operations succeeded, log a warning but don't fail the entire sync
            if (successfulOperations < totalOperations) {
                log.warn("Tool inventory sync completed with some failures - successRate={}%, failedOperations={}", 
                        (successfulOperations * 100.0 / totalOperations), (totalOperations - successfulOperations));
            }

        } catch (ValidationException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Validation error during tool inventory sync - operation=inventory_sync, processingTime={}ms, errorType=ValidationException", 
                    processingTime, e);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            // Comprehensive error logging with context information
            log.error("Unexpected error during tool inventory sync - operation=inventory_sync, processingTime={}ms, errorType={}, agentToolCount={}", 
                    processingTime, e.getClass().getSimpleName(), currentToolInfos.size(), e);
            throw new RuntimeException("Failed to sync tool inventory from agent", e);
        }
    }





    /**
     * Resolves MCP server ID from server name.
     * For built-in tools, returns ID 0. For other servers, looks up by name.
     * 
     * @param mcpServerName the name of the MCP server
     * @return the MCP server ID
     * @throws RuntimeException if server name is not found
     */
    private Long resolveMcpServerId(String mcpServerName) {
        // Look up MCP server by name
        try {
            return mcpServerService.findByServerName(mcpServerName).getId();
        } catch (Exception e) {
            log.error("Failed to resolve MCP server ID for server name: {}", mcpServerName, e);
            throw new RuntimeException("Unknown MCP server: " + mcpServerName);
        }
    }

}