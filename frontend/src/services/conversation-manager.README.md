# Conversation Management System

The conversation management system provides a structured way to handle Discord bot interactions with the Lenza AI agent backend. It manages conversation context extraction, request payload building, and backend API integration.

## Overview

The system consists of several key components:

- **ConversationManager**: Core class for managing conversation context and payloads
- **ConversationContext**: Structure containing user, channel, and server information
- **Payload Builders**: Functions for creating new and resume conversation requests
- **Backend Integration**: Conversion to backend API request format
- **Validation**: Input validation for context and messages

## Key Features

### 1. Conversation Context Extraction

Extracts conversation context from Discord interactions:

```typescript
import { conversationManager } from '../services';

const context = conversationManager.extractConversationContext(interaction);
// Returns: { serverId, channelId, memberId, guildId, username, channelName?, guildName? }
```

### 2. New Conversation Payloads

Build payloads for starting new conversations:

```typescript
const payload = conversationManager.buildNewConversationPayload(
  context,
  'Hello, Lenza!',
  ['tool1', 'tool2'] // optional tool whitelist
);
```

### 3. Resume Conversation Payloads

Build payloads for continuing existing conversations:

```typescript
// With message
const payload = conversationManager.buildResumeConversationPayload(
  context,
  'Continue our chat'
);

// With approval decision
const payload = conversationManager.buildResumeConversationPayload(
  context,
  undefined, // no message
  true,      // approved
  ['new-tool'] // tool whitelist update
);
```

### 4. Backend API Integration

Convert payloads to backend request format:

```typescript
const backendRequest = conversationManager.convertToBackendRequest(payload);
// Converts Discord string IDs to numbers and formats for backend
```

### 5. Validation

Validate conversation context and messages:

```typescript
// Validate context
conversationManager.validateConversationContext(context);

// Validate message
conversationManager.validateMessage('Hello, Lenza!');
```

## Usage Examples

### Basic New Conversation Command

```typescript
import { SlashCommandBuilder, ChatInputCommandInteraction } from 'discord.js';
import { conversationManager, BackendApiClient } from '../services';
import { extractConversationContext } from '../utils/conversation-utils';

export const lenzaNewCommand = {
  data: new SlashCommandBuilder()
    .setName('lenza-new')
    .setDescription('Start a new conversation with Lenza')
    .addStringOption(option =>
      option.setName('message')
        .setDescription('Your message to Lenza')
        .setRequired(true)
    ),

  async execute(interaction: ChatInputCommandInteraction) {
    await interaction.deferReply();

    // Extract context and build payload
    const context = extractConversationContext(interaction);
    const userMessage = interaction.options.getString('message', true);
    const payload = conversationManager.buildNewConversationPayload(context, userMessage);

    // Stream response from backend
    const apiClient = new BackendApiClient();
    let responseContent = '';
    
    for await (const response of apiClient.invokeNewConversation(payload)) {
      responseContent += response.content;
      await interaction.editReply(responseContent);
    }
  }
};
```

### Tool Approval Handling

```typescript
async function handleToolApproval(
  interaction: ChatInputCommandInteraction,
  approved: boolean,
  addToWhitelist: boolean = false
) {
  const context = extractConversationContext(interaction);
  
  const payload = conversationManager.buildResumeConversationPayload(
    context,
    undefined, // no message
    approved,
    addToWhitelist ? ['approved-tool'] : undefined
  );

  const apiClient = new BackendApiClient();
  
  for await (const response of apiClient.resumeConversation(payload)) {
    // Handle streaming response
    await interaction.editReply(response.content);
  }
}
```

## Data Structures

### ConversationContext

```typescript
interface ConversationContext {
  serverId: string;      // Discord server ID
  channelId: string;     // Discord channel ID
  memberId: string;      // Discord user ID
  guildId: string;       // Same as serverId
  username: string;      // Discord username
  channelName?: string;  // Channel name (optional)
  guildName?: string;    // Guild name (optional)
}
```

### NewConversationPayload

```typescript
interface NewConversationPayload {
  serverId: string;
  channelId: string;
  memberId: string;
  message: string;
  toolWhitelist?: string[];
}
```

### ResumeConversationPayload

```typescript
interface ResumeConversationPayload {
  serverId: string;
  channelId: string;
  memberId: string;
  message?: string;
  approved?: boolean;
  toolWhitelistUpdate?: string[];
}
```

### BackendConversationRequest

```typescript
interface BackendConversationRequest {
  message: string;
  channelId: number;        // Converted from string
  memberId: number;         // Converted from string
  serverId?: number;        // Converted from string
  approved?: boolean;
  toolWhitelistUpdate?: string[];
}
```

## Utility Functions

The system includes utility functions for common operations:

```typescript
import { 
  extractConversationContext, 
  validateConversationMessage,
  formatContextSummary,
  isValidGuildInteraction,
  extractUserMessage
} from '../utils/conversation-utils';

// Extract and validate context
const context = extractConversationContext(interaction);

// Validate message
validateConversationMessage(userMessage);

// Format context for logging
const summary = formatContextSummary(context);

// Check if interaction is valid
if (isValidGuildInteraction(interaction)) {
  // Process interaction
}

// Extract user message from interaction
const message = extractUserMessage(interaction, 'message');
```

## Error Handling

The system provides comprehensive error handling:

- **Invalid Context**: Throws errors for missing guild, channel, or user
- **Invalid IDs**: Validates Discord snowflake ID format
- **Message Validation**: Checks for empty messages and Discord character limits
- **ID Conversion**: Handles potential overflow when converting to numbers

## Integration with Backend API Client

The conversation management system integrates seamlessly with the BackendApiClient:

```typescript
const apiClient = new BackendApiClient();

// New conversation
for await (const response of apiClient.invokeNewConversation(newPayload)) {
  // Handle streaming response
}

// Resume conversation
for await (const response of apiClient.resumeConversation(resumePayload)) {
  // Handle streaming response
}
```

## Requirements Fulfilled

This implementation fulfills the following task requirements:

✅ **Create conversation context structure with user, channel, and server IDs for API requests**
- ConversationContext interface with all required IDs
- Extraction from Discord interactions
- Validation of context data

✅ **Add utility functions for building conversation request payloads**
- buildNewConversationPayload() for new conversations
- buildResumeConversationPayload() for continuing conversations
- convertToBackendRequest() for backend API format

✅ **Create conversation context extraction from Discord interactions**
- extractConversationContext() method
- Utility functions in conversation-utils.ts
- Error handling for invalid interactions

✅ **Backend handles all conversation ID generation and tracking**
- System passes user/channel/server context to backend
- Backend responsible for conversation ID management
- No client-side conversation ID storage

The system is designed to be type-safe, well-documented, and easy to use in Discord command implementations.