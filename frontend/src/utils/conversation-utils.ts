/**
 * Utility functions for conversation management
 */

import { ChatInputCommandInteraction } from 'discord.js';
import { conversationManager, ConversationContext } from '../services/conversation-manager';
import { logger } from './logger';

/**
 * Extract and validate conversation context from Discord interaction
 */
export function extractConversationContext(interaction: ChatInputCommandInteraction): ConversationContext {
  try {
    const context = conversationManager.extractConversationContext(interaction);
    conversationManager.validateConversationContext(context);
    return context;
  } catch (error) {
    logger.error('Failed to extract conversation context:', error);
    throw new Error('Unable to extract conversation context from interaction');
  }
}

/**
 * Validate message content for conversation
 */
export function validateConversationMessage(message: string): void {
  try {
    conversationManager.validateMessage(message);
  } catch (error) {
    logger.error('Message validation failed:', error);
    throw error;
  }
}

/**
 * Create a formatted context summary for logging
 */
export function formatContextSummary(context: ConversationContext): string {
  return `${context.username} in #${context.channelName || 'unknown'} (${context.guildName || 'unknown'})`;
}

/**
 * Check if interaction is from a valid guild context
 */
export function isValidGuildInteraction(interaction: ChatInputCommandInteraction): boolean {
  return !!(interaction.guild && interaction.channel && interaction.user);
}

/**
 * Extract user message from interaction options
 */
export function extractUserMessage(interaction: ChatInputCommandInteraction, optionName: string = 'message'): string {
  const message = interaction.options.getString(optionName, true);
  validateConversationMessage(message);
  return message;
}