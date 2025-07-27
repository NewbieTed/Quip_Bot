package com.quip.backend.tool.controller;

import com.quip.backend.tool.service.ToolService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing tools.
 * <p>
 * This controller provides endpoints for registering, retrieving, and managing tools.
 * It handles HTTP requests related to tool management and delegates
 * business logic to the ToolService.
 * </p>
 * <p>
 * Endpoints include:
 * - Registering new tools
 * - Retrieving tool information
 * - Updating tool settings
 * - Enabling/disabling tools
 * - Getting available tools for members
 * - Bulk tool operations
 * </p>
 */
@Validated
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolController {
    /**
     * Service for tool-related operations.
     */
    private final ToolService toolService;
    

}