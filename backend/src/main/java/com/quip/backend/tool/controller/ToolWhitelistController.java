package com.quip.backend.tool.controller;

import com.quip.backend.tool.service.ToolWhitelistService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing tool whitelist permissions.
 * <p>
 * This controller provides endpoints for managing tool access permissions for members.
 * It handles HTTP requests related to tool whitelist management and delegates
 * business logic to the ToolWhitelistService.
 * </p>
 * <p>
 * Endpoints include:
 * - Adding tools to member whitelists
 * - Removing tools from member whitelists
 * - Retrieving whitelist entries
 * - Checking tool permissions
 * - Managing whitelist scopes and expiration
 * - Bulk whitelist operations
 * </p>
 */
@Validated
@RestController
@RequestMapping("/tool-whitelist")
@RequiredArgsConstructor
public class ToolWhitelistController {
    /**
     * Service for tool whitelist-related operations.
     */
    private final ToolWhitelistService toolWhitelistService;

}