/**
 * Conversation management system for Discord bot
 * Handles conversation context extraction and request payload building
 */

import { ChatInputCommandInteraction } from 'discord.js';
import { logger } from '../utils/logger';

/**
 * Conversation context structure with user, channel, and server IDs
 */
export interface ConversationContext {
  serverId: string;
  channelId: string;
  memberId: string;
  guildId: string;
  username: string;
  channelName?: string;
  guildName?: string;
}

/**
 * Request payload for new conversation
 */
export interface NewConversationPayload {
  serverId: string;
  channelId: string;
  memberId: string;
  message: string;
  toolWhitelist?: string[];
}

/**
 * Request payload for resuming conversation
 */
export interface ResumeConversationPayload {
  serverId: string;
  channelId: string;
  memberId: string;
  message?: string;
  approved?: boolean;
  toolWhitelistUpdate?: string[];
}

/**
 * Backend API request format (matches backend expectations)
 */
export interface BackendConversationRequest {
  message: string;
  channelId: number;
  memberId: number;
  serverId?: number;
  approved?: boolean;
  toolWhitelistUpdate?: string[];
}

/**
 * Conversation Manager class for handling conversation context and payloads
 */
export class ConversationManager {
  
  /**
   * Extract conversation context from Discord interaction
   */
  extractConversationContext(interaction: ChatInputCommandInteraction): ConversationContext {
    if (!interaction.guild) {
      throw new Error('Interaction must be from a guild (server)');
    }

    if (!interaction.channel) {
      throw new Error('Interaction must be from a channel');
    }

    const context: ConversationContext = {
      serverId: interaction.guild.id,
      channelId: interaction.channel.id,
      memberId: interaction.user.id,
      guildId: interaction.guild.id,
      username: interaction.user.username,
      channelName: interaction.channel.type === 0 ? interaction.channel.name : undefined,
      guildName: interaction.guild.name
    };

    logger.debug(`Extracted conversation context: serverId=${context.serverId}, channelId=${context.channelId}, memberId=${context.memberId}, username=${context.username}`);

    return context;
  }

  /**
   * Build payload for new conversation request
   */
  buildNewConversationPayload(
    context: ConversationContext, 
    message: string, 
    toolWhitelist?: string[]
  ): NewConversationPayload {
    const payload: NewConversationPayload = {
      serverId: context.serverId,
      channelId: context.channelId,
      memberId: context.memberId,
      message: message.trim(),
      toolWhitelist: toolWhitelist || []
    };

    logger.debug(`Built new conversation payload: serverId=${payload.serverId}, channelId=${payload.channelId}, memberId=${payload.memberId}, messageLength=${payload.message.length}, toolWhitelistCount=${payload.toolWhitelist?.length || 0}`);

    return payload;
  }

  /**
   * Build payload for resuming conversation request
   */
  buildResumeConversationPayload(
    context: ConversationContext,
    message?: string,
    approved?: boolean,
    toolWhitelistUpdate?: string[]
  ): ResumeConversationPayload {
    const payload: ResumeConversationPayload = {
      serverId: context.serverId,
      channelId: context.channelId,
      memberId: context.memberId
    };

    if (message !== undefined) {
      payload.message = message.trim();
    }

    if (approved !== undefined) {
      payload.approved = approved;
    }

    if (toolWhitelistUpdate !== undefined) {
      payload.toolWhitelistUpdate = toolWhitelistUpdate;
    }

    logger.debug(`Built resume conversation payload: serverId=${payload.serverId}, channelId=${payload.channelId}, memberId=${payload.memberId}, hasMessage=${payload.message !== undefined}, approved=${payload.approved}, toolWhitelistUpdateCount=${payload.toolWhitelistUpdate?.length || 0}`);

    return payload;
  }

  /**
   * Convert conversation payload to backend API request format
   */
  convertToBackendRequest(
    payload: NewConversationPayload | ResumeConversationPayload
  ): BackendConversationRequest {
    // Convert string IDs to numbers for backend compatibility
    const channelIdNum = this.parseDiscordId(payload.channelId);
    const memberIdNum = this.parseDiscordId(payload.memberId);
    const serverIdNum = this.parseDiscordId(payload.serverId);

    const backendRequest: BackendConversationRequest = {
      message: payload.message || '',
      channelId: channelIdNum,
      memberId: memberIdNum,
      serverId: serverIdNum
    };

    // Add optional fields for resume requests
    if ('approved' in payload && payload.approved !== undefined) {
      backendRequest.approved = payload.approved;
    }

    if ('toolWhitelistUpdate' in payload && payload.toolWhitelistUpdate !== undefined) {
      backendRequest.toolWhitelistUpdate = payload.toolWhitelistUpdate;
    }

    logger.debug(`Converted to backend request format: channelId=${backendRequest.channelId}, memberId=${backendRequest.memberId}, serverId=${backendRequest.serverId}, messageLength=${backendRequest.message.length}, approved=${backendRequest.approved}, toolWhitelistUpdateCount=${backendRequest.toolWhitelistUpdate?.length || 0}`);

    return backendRequest;
  }

  /**
   * Parse Discord snowflake ID to number
   * Discord IDs are 64-bit integers, but we need to handle potential overflow
   */
  private parseDiscordId(discordId: string): number {
    const parsed = parseInt(discordId, 10);
    
    if (isNaN(parsed)) {
      throw new Error(`Invalid Discord ID: ${discordId}`);
    }

    // Check for potential overflow (JavaScript safe integer limit)
    if (parsed > Number.MAX_SAFE_INTEGER) {
      logger.warn(`Discord ID ${discordId} exceeds safe integer limit, potential precision loss`);
    }

    return parsed;
  }

  /**
   * Validate conversation context
   */
  validateConversationContext(context: ConversationContext): void {
    if (!context.serverId || !context.channelId || !context.memberId) {
      throw new Error('Conversation context must include serverId, channelId, and memberId');
    }

    if (!context.username) {
      throw new Error('Conversation context must include username');
    }

    // Validate Discord ID format (snowflakes are numeric strings)
    const idPattern = /^\d+$/;
    
    if (!idPattern.test(context.serverId)) {
      throw new Error(`Invalid serverId format: ${context.serverId}`);
    }
    
    if (!idPattern.test(context.channelId)) {
      throw new Error(`Invalid channelId format: ${context.channelId}`);
    }
    
    if (!idPattern.test(context.memberId)) {
      throw new Error(`Invalid memberId format: ${context.memberId}`);
    }

    logger.debug('Conversation context validation passed');
  }

  /**
   * Validate message content
   */
  validateMessage(message: string): void {
    if (!message || message.trim().length === 0) {
      throw new Error('Message cannot be empty');
    }

    if (message.length > 2000) {
      throw new Error('Message exceeds Discord character limit (2000)');
    }

    logger.debug(`Message validation passed: messageLength=${message.length}`);
  }

  /**
   * Create conversation context from raw IDs (for testing or manual creation)
   */
  createConversationContext(
    serverId: string,
    channelId: string,
    memberId: string,
    username: string,
    channelName?: string,
    guildName?: string
  ): ConversationContext {
    const context: ConversationContext = {
      serverId,
      channelId,
      memberId,
      guildId: serverId, // guildId is same as serverId
      username,
      channelName,
      guildName
    };

    this.validateConversationContext(context);
    
    return context;
  }
}

/**
 * Default conversation manager instance
 */
export const conversationManager = new ConversationManager();