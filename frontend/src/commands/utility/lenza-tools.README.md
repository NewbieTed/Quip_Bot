# Lenza Tools Command

The `/lenza-tools` command provides comprehensive tool whitelist management for Lenza AI interactions.

## Overview

This command allows users to manage which tools Lenza can use automatically without requiring manual approval each time. Tools can be whitelisted at different scopes (global, server, or conversation) to provide fine-grained control over tool permissions.

## Subcommands

### `/lenza-tools view`
View your current tool whitelist.

**Usage:**
```
/lenza-tools view
```

**Description:**
Displays information about your current tool whitelist and available actions. Currently shows a coming soon message as the backend GET endpoint for retrieving whitelist data needs to be implemented.

### `/lenza-tools add`
Add a tool to your whitelist.

**Usage:**
```
/lenza-tools add tool:<tool-name> [scope:<scope>]
```

**Parameters:**
- `tool` (required): Name of the tool to add to your whitelist
- `scope` (optional): Scope of the whitelist entry
  - `global`: Available across all servers (default: server)
  - `server`: Available only in this server
  - `conversation`: Available only in specific conversations

**Examples:**
```
/lenza-tools add tool:weather-api
/lenza-tools add tool:file-search scope:global
/lenza-tools add tool:database-query scope:server
```

### `/lenza-tools remove`
Remove a tool from your whitelist.

**Usage:**
```
/lenza-tools remove tool:<tool-name> [scope:<scope>]
```

**Parameters:**
- `tool` (required): Name of the tool to remove from your whitelist
- `scope` (optional): Scope of the whitelist entry to remove (default: server)
  - `global`: Remove from global scope
  - `server`: Remove from server scope
  - `conversation`: Remove from conversation scope

**Examples:**
```
/lenza-tools remove tool:weather-api
/lenza-tools remove tool:file-search scope:global
```

### `/lenza-tools clear`
Clear tools from your whitelist.

**Usage:**
```
/lenza-tools clear [scope:<scope>]
```

**Parameters:**
- `scope` (optional): Scope to clear (if not specified, shows confirmation for all scopes)
  - `global`: Clear only global scoped tools
  - `server`: Clear only server scoped tools
  - `conversation`: Clear only conversation scoped tools

**Examples:**
```
/lenza-tools clear
/lenza-tools clear scope:server
```

**Note:** This feature shows a confirmation dialog but currently displays a "coming soon" message as it requires additional backend support to retrieve and clear existing whitelist entries.

## Tool Whitelist Scopes

### Global Scope
- Tools are available across all Discord servers
- Highest permission level
- Affects all conversations globally
- Use with caution for trusted tools only

### Server Scope (Default)
- Tools are available only within the current Discord server
- Balanced permission level
- Good for server-specific tools and workflows
- Recommended for most use cases

### Conversation Scope
- Tools are available only within specific conversations
- Lowest permission level
- Most restrictive and secure
- Good for temporary or experimental tool access

## Backend Integration

The command integrates with the Spring Boot backend via the `/tool-whitelist/update` endpoint, sending requests in the format expected by `UpdateToolWhitelistRequestDto`.

### Request Structure
```typescript
{
  memberId: number,
  channelId: number,
  addRequests?: [{
    toolName: string,
    scope: 'GLOBAL' | 'SERVER' | 'CONVERSATION',
    agentConversationId?: number | null,
    expiresAt?: string | null
  }],
  removeRequests?: [{
    toolName: string,
    scope: 'GLOBAL' | 'SERVER' | 'CONVERSATION',
    agentConversationId?: number | null
  }]
}
```

## Error Handling

The command includes comprehensive error handling for:
- Network connectivity issues
- Invalid tool names (empty, too long)
- Backend API failures
- Validation errors
- Permission issues

## Security Considerations

- Only the user who initiated the command can manage their own whitelist
- Tool names are validated and sanitized
- Scope permissions are enforced by the backend
- All requests include proper authorization context

## Future Enhancements

1. **View Command**: Implement backend GET endpoint to retrieve current whitelist
2. **Clear Command**: Implement bulk clear functionality with backend support
3. **Tool Discovery**: Add ability to browse available tools
4. **Expiration Management**: Support for temporary tool approvals
5. **Bulk Operations**: Support for adding/removing multiple tools at once

## Testing

The command includes comprehensive tests:
- Unit tests: `lenza-tools-command-test.ts`
- Integration tests: `lenza-tools-integration-test.ts`

Run tests with:
```bash
npx ts-node src/test/lenza-tools-command-test.ts
npx ts-node src/test/lenza-tools-integration-test.ts
```