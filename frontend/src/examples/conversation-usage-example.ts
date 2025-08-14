/**
 * Example usage of the conversation management system
 * This demonstrates how to use the conversation manager in Discord commands
 */

import { SlashCommandBuilder, ChatInputCommandInteraction } from 'discord.js';
import { conversationManager, BackendApiClient } from '../services';
import { extractConversationContext, validateConversationMessage } from '../utils/conversation-utils';
import { logger } from '../utils/logger';

/**
 * Example implementation of /lenza-new command using conversation management
 */
export const lenzaNewCommandExample = {
  data: new SlashCommandBuilder()
    .setName('lenza-new')
    .setDescription('Start a new conversation with Lenza')
    .addStringOption(option =>
      option.setName('message')
        .setDescription('Your message to Lenza')
        .setRequired(true)
    ),

  async execute(interaction: ChatInputCommandInteraction) {
    try {
      // Defer reply to prevent timeout
      await interaction.deferReply();

      // Extract conversation context from Discord interaction
      const context = extractConversationContext(interaction);
      
      // Get user message and validate it
      const userMessage = interaction.options.getString('message', true);
      validateConversationMessage(userMessage);

      // Build new conversation payload
      const payload = conversationManager.buildNewConversationPayload(
        context,
        userMessage,
        [] // Empty tool whitelist for new conversations
      );

      // Initialize backend API client
      const apiClient = new BackendApiClient();

      // Start streaming response
      let responseContent = '';
      
      try {
        for await (const response of apiClient.invokeNewConversation(payload)) {
          responseContent += response.content;
          
          // Update Discord message with streaming content
          if (responseContent.length > 0) {
            // Truncate if too long for Discord
            const displayContent = responseContent.length > 2000 
              ? responseContent.substring(0, 1997) + '...'
              : responseContent;
              
            await interaction.editReply(displayContent);
          }
        }
      } catch (streamError) {
        logger.error('Error during streaming response', streamError);
        await interaction.editReply('Sorry, there was an error processing your request.');
      }

    } catch (error) {
      logger.error('Error in lenza-new command', error);
      
      if (interaction.deferred) {
        await interaction.editReply('Sorry, there was an error processing your request.');
      } else {
        await interaction.reply({ 
          content: 'Sorry, there was an error processing your request.', 
          ephemeral: true 
        });
      }
    }
  }
};

/**
 * Example implementation of /lenza-resume command using conversation management
 */
export const lenzaResumeCommandExample = {
  data: new SlashCommandBuilder()
    .setName('lenza-resume')
    .setDescription('Continue an existing conversation with Lenza')
    .addStringOption(option =>
      option.setName('message')
        .setDescription('Your message to Lenza')
        .setRequired(true)
    ),

  async execute(interaction: ChatInputCommandInteraction) {
    try {
      // Defer reply to prevent timeout
      await interaction.deferReply();

      // Extract conversation context from Discord interaction
      const context = extractConversationContext(interaction);
      
      // Get user message and validate it
      const userMessage = interaction.options.getString('message', true);
      validateConversationMessage(userMessage);

      // Build resume conversation payload
      const payload = conversationManager.buildResumeConversationPayload(
        context,
        userMessage
      );

      // Initialize backend API client
      const apiClient = new BackendApiClient();

      // Start streaming response
      let responseContent = '';
      
      try {
        for await (const response of apiClient.resumeConversation(payload)) {
          responseContent += response.content;
          
          // Update Discord message with streaming content
          if (responseContent.length > 0) {
            // Truncate if too long for Discord
            const displayContent = responseContent.length > 2000 
              ? responseContent.substring(0, 1997) + '...'
              : responseContent;
              
            await interaction.editReply(displayContent);
          }
        }
      } catch (streamError) {
        logger.error('Error during streaming response', streamError);
        await interaction.editReply('Sorry, there was an error processing your request.');
      }

    } catch (error) {
      logger.error('Error in lenza-resume command', error);
      
      if (interaction.deferred) {
        await interaction.editReply('Sorry, there was an error processing your request.');
      } else {
        await interaction.reply({ 
          content: 'Sorry, there was an error processing your request.', 
          ephemeral: true 
        });
      }
    }
  }
};

/**
 * Example of handling tool approval using conversation management
 */
export async function handleToolApprovalExample(
  interaction: ChatInputCommandInteraction,
  approved: boolean,
  addToWhitelist: boolean = false
) {
  try {
    // Extract conversation context
    const context = extractConversationContext(interaction);

    // Build resume payload with approval decision
    const payload = conversationManager.buildResumeConversationPayload(
      context,
      undefined, // No message for approval
      approved,
      addToWhitelist ? ['approved-tool'] : undefined // Example tool whitelist update
    );

    // Send approval decision to backend
    const apiClient = new BackendApiClient();
    
    // Continue conversation with approval decision
    let responseContent = '';
    
    for await (const response of apiClient.resumeConversation(payload)) {
      responseContent += response.content;
      
      if (responseContent.length > 0) {
        const displayContent = responseContent.length > 2000 
          ? responseContent.substring(0, 1997) + '...'
          : responseContent;
          
        await interaction.editReply(displayContent);
      }
    }

  } catch (error) {
    logger.error('Error handling tool approval', error);
    await interaction.editReply('Sorry, there was an error processing the approval.');
  }
}

/**
 * Example of creating conversation context manually (for testing)
 */
export function createTestConversationContext() {
  return conversationManager.createConversationContext(
    '123456789012345678', // serverId
    '987654321098765432', // channelId
    '111222333444555666', // memberId
    'testuser',           // username
    'test-channel',       // channelName (optional)
    'Test Guild'          // guildName (optional)
  );
}

/**
 * Example of validating conversation data
 */
export function validateConversationDataExample() {
  try {
    // Create test context
    const context = createTestConversationContext();
    
    // Validate context
    conversationManager.validateConversationContext(context);
    logger.info('Context validation passed');
    
    // Validate message
    const testMessage = 'Hello, Lenza!';
    conversationManager.validateMessage(testMessage);
    logger.info('Message validation passed');
    
    return true;
  } catch (error) {
    logger.error('Validation failed', error);
    return false;
  }
}