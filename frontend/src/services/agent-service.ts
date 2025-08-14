import { ChatInputCommandInteraction } from 'discord.js';
import { BackendApiClient } from './backend-api-client';
import { StreamingResponseHandler } from './streaming-response-handler';
import { BackendAgentRequest } from '../models/agent-models';
import { AgentError, getUserFriendlyErrorMessage } from '../errors/agent-error';
import { logger } from '../utils/logger';

/**
 * High-level service for agent interactions that combines API client and streaming handler
 */
export class AgentService {
  private apiClient: BackendApiClient;

  constructor() {
    this.apiClient = new BackendApiClient();
  }

  /**
   * Invoke agent for continuing existing conversation
   */
  async invokeAgent(
    interaction: ChatInputCommandInteraction,
    message: string
  ): Promise<void> {
    const request = this.buildBackendRequest(interaction, message);
    const streamHandler = new StreamingResponseHandler(interaction);

    try {
      logger.info(`Invoking agent for user ${interaction.user.id} in channel ${interaction.channelId}`);
      
      const responseStream = this.apiClient.invokeAgent(request);
      await streamHandler.handleStream(responseStream);
      
      logger.info('Agent invocation completed successfully');
    } catch (error) {
      logger.error('Agent invocation failed:', error);
      await this.handleAgentError(interaction, error);
    }
  }

  /**
   * Invoke agent for new conversation
   */
  async invokeNewAgent(
    interaction: ChatInputCommandInteraction,
    message: string
  ): Promise<void> {
    const request = this.buildBackendRequest(interaction, message);
    const streamHandler = new StreamingResponseHandler(interaction);

    try {
      logger.info(`Invoking new agent for user ${interaction.user.id} in channel ${interaction.channelId}`);
      
      const responseStream = this.apiClient.invokeNewAgent(request);
      await streamHandler.handleStream(responseStream);
      
      logger.info('New agent invocation completed successfully');
    } catch (error) {
      logger.error('New agent invocation failed:', error);
      await this.handleAgentError(interaction, error);
    }
  }

  /**
   * Send approval decision to backend
   */
  async sendApprovalDecision(
    interaction: ChatInputCommandInteraction,
    approved: boolean
  ): Promise<void> {
    const request = this.buildBackendRequest(interaction, '');
    // Add approval decision to request - this will need to be adjusted based on final backend API
    (request as any).approved = approved;

    try {
      logger.info(`Sending approval decision: ${approved} for user ${interaction.user.id}`);
      
      await this.apiClient.sendApprovalDecision(request);
      
      logger.info('Approval decision sent successfully');
    } catch (error) {
      logger.error('Failed to send approval decision:', error);
      throw error; // Let the caller handle this error
    }
  }

  /**
   * Build backend request from Discord interaction
   */
  private buildBackendRequest(
    interaction: ChatInputCommandInteraction,
    message: string
  ): BackendAgentRequest {
    return {
      message,
      channelId: parseInt(interaction.channelId || '0'),
      memberId: parseInt(interaction.user.id)
    };
  }

  /**
   * Handle agent errors by showing user-friendly messages
   */
  private async handleAgentError(
    interaction: ChatInputCommandInteraction,
    error: unknown
  ): Promise<void> {
    let errorMessage: string;

    if (error instanceof AgentError) {
      errorMessage = getUserFriendlyErrorMessage(error);
    } else {
      errorMessage = "An unexpected error occurred. Please try again.";
    }

    try {
      if (interaction.replied || interaction.deferred) {
        await interaction.editReply(errorMessage);
      } else {
        await interaction.reply({
          content: errorMessage,
          ephemeral: true
        });
      }
    } catch (replyError) {
      logger.error('Failed to send error message to user:', replyError);
    }
  }
}