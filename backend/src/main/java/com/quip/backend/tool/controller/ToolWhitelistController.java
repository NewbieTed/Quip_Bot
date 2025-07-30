package com.quip.backend.tool.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto;
import com.quip.backend.tool.service.ToolWhitelistService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Updates tool whitelist for a member and notifies the agent service.
     * <p>
     * This endpoint processes tool whitelist update requests, which can include
     * both additions and removals of tools from a member's whitelist. After
     * updating the database, it notifies the agent service about the changes.
     * </p>
     *
     * @param updateRequest the request containing whitelist changes
     * @return BaseResponse indicating success
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateToolWhitelist(
            @Valid @RequestBody UpdateToolWhitelistRequestDto updateRequest) {
        
        toolWhitelistService.updateToolWhitelist(updateRequest);
        return BaseResponse.success();
    }
}