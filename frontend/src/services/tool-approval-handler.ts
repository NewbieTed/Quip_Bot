import { 
  ChatInputCommandInteraction, 
  ButtonInteraction, 
  ActionRowBuilder, 
  ButtonBuilder, 
  ButtonStyle,
  ComponentType,
  Message
} from 'discord.js';
import { BackendApiClient } from './backend-api-client';
import { ConversationManager } from './conversation-manager';
import { 
  ToolApprovalRequest, 
  ApprovalResult, 
  ResumeConversationPayload 
} from '../models/agent-models';
import { getBotConfig } from '../config/bot-config';
import { logger } from '../utils/logger';

/**
 * Handles tool approval requests with interactive buttons and timeout management
 */
export class ToolApprovalHandler {
  private config = getBotConfig();
  private apiClient: BackendApiClient;
  private conversationManager: ConversationManager;
  private activeApprovals = new Map<string, ToolApprovalRequest>();

  constructor() {
    this.apiClient = new BackendApiClient();
    this.conversationManager = new ConversationManager();
  }

  /**
   * Request approval for a tool with interactive buttons
   */
  async requestApproval(
    originalInteraction: ChatInputCommandInteraction,
    toolName: string,
    description: string
  ): Promise<ApprovalResult> {
    const userId = originalInteraction.user.id;
    const approvalId = this.generateApprovalId(userId, toolName);
    
    logger.info(`Requesting tool approval: ${toolName} for user ${userId}`);

    return new Promise<ApprovalResult>((resolve) => {
      // Create timeout for automatic denial
      const timeoutId = setTimeout(() => {
        this.handleTimeout(approvalId, resolve);
      }, this.config.approvalTimeoutSeconds * 1000);

      // Store approval request
      const approvalRequest: ToolApprovalRequest = {
        approvalId,
        userId,
        toolName,
        description,
        originalInteraction,
        timeoutId,
        resolve
      };

      this.activeApprovals.set(approvalId, approvalRequest);

      // Send approval message with buttons
      this.sendApprovalMessage(approvalRequest).catch(error => {
        logger.error('Failed to send approval message:', error);
        this.cleanupApproval(approvalId);
        resolve({
          approved: false,
          addToWhitelist: false,
          timedOut: false,
          error: 'Failed to send approval request'
        });
      });
    });
  }

  /**
   * Handle button interaction for approval responses
   */
  async handleApprovalResponse(interaction: ButtonInteraction): Promise<void> {
    const customId = interaction.customId;
    
    if (!customId.startsWith('tool_approval_')) {
      return; // Not a tool approval button
    }

    const [, , action, approvalId] = customId.split('_');
    const approvalRequest = this.activeApprovals.get(approvalId);

    if (!approvalRequest) {
      await interaction.reply({
        content: '❌ This approval request has expired or is no longer valid.',
        ephemeral: true
      });
      return;
    }

    // Verify that only the original requester can approve
    if (interaction.user.id !== approvalRequest.userId) {
      await interaction.reply({
        content: '❌ Only the original requester can approve or deny this tool.',
        ephemeral: true
      });
      return;
    }

    // Acknowledge the interaction immediately
    await interaction.deferUpdate();

    try {
      let result: ApprovalResult;

      switch (action) {
        case 'approve':
          result = await this.handleApprove(approvalRequest, false);
          break;
        case 'deny':
          result = await this.handleDeny(approvalRequest);
          break;
        case 'trust':
          result = await this.handleApprove(approvalRequest, true);
          break;
        default:
          logger.warn(`Unknown approval action: ${action}`);
          result = {
            approved: false,
            addToWhitelist: false,
            timedOut: false,
            error: 'Unknown action'
          };
      }

      // Update the message to show the result
      await this.updateApprovalMessage(interaction, approvalRequest, result);

      // Resolve the promise
      approvalRequest.resolve(result);

    } catch (error) {
      logger.error('Error handling approval response:', error);
      
      const errorResult: ApprovalResult = {
        approved: false,
        addToWhitelist: false,
        timedOut: false,
        error: 'Failed to process approval'
      };

      await this.updateApprovalMessage(interaction, approvalRequest, errorResult);
      approvalRequest.resolve(errorResult);
    } finally {
      // Clean up the approval request
      this.cleanupApproval(approvalId);
    }
  }

  /**
   * Send approval message with interactive buttons
   */
  private async sendApprovalMessage(approvalRequest: ToolApprovalRequest): Promise<void> {
    const { toolName, description, originalInteraction, approvalId } = approvalRequest;

    const embed = {
      color: 0xFFA500, // Orange color for approval requests
      title: '🔧 Tool Approval Required',
      description: `Lenza wants to use the **${toolName}** tool.`,
      fields: [
        {
          name: 'Description',
          value: description || 'No description provided',
          inline: false
        },
        {
          name: 'What would you like to do?',
          value: '• **Approve** - Allow this tool for this request only\n• **Deny** - Reject this tool request\n• **Approve & Trust** - Allow this tool and add to your whitelist for future automatic approval',
          inline: false
        }
      ],
      footer: {
        text: `This request will automatically be denied in ${this.config.approvalTimeoutSeconds} seconds if no action is taken.`
      },
      timestamp: new Date().toISOString()
    };

    const buttons = this.createApprovalButtons(approvalId);

    try {
      if (originalInteraction.replied || originalInteraction.deferred) {
        await originalInteraction.editReply({
          content: '',
          embeds: [embed],
          components: [buttons]
        });
      } else {
        await originalInteraction.reply({
          embeds: [embed],
          components: [buttons],
          fetchReply: true
        });
      }
    } catch (error) {
      logger.error('Failed to send approval message:', error);
      throw error;
    }
  }

  /**
   * Create approval buttons
   */
  private createApprovalButtons(approvalId: string): ActionRowBuilder<ButtonBuilder> {
    return new ActionRowBuilder<ButtonBuilder>()
      .addComponents(
        new ButtonBuilder()
          .setCustomId(`tool_approval_approve_${approvalId}`)
          .setLabel('Approve')
          .setStyle(ButtonStyle.Success)
          .setEmoji('✅'),
        new ButtonBuilder()
          .setCustomId(`tool_approval_deny_${approvalId}`)
          .setLabel('Deny')
          .setStyle(ButtonStyle.Danger)
          .setEmoji('❌'),
        new ButtonBuilder()
          .setCustomId(`tool_approval_trust_${approvalId}`)
          .setLabel('Approve & Trust')
          .setStyle(ButtonStyle.Primary)
          .setEmoji('🔒')
      );
  }

  /**
   * Handle approve action
   */
  private async handleApprove(
    approvalRequest: ToolApprovalRequest, 
    addToWhitelist: boolean
  ): Promise<ApprovalResult> {
    const { originalInteraction, toolName } = approvalRequest;

    try {
      // Send approval to backend
      await this.sendApprovalToBackend(originalInteraction, true, addToWhitelist ? [toolName] : []);

      logger.info(`Tool approved: ${toolName}, addToWhitelist: ${addToWhitelist}`);

      return {
        approved: true,
        addToWhitelist,
        timedOut: false
      };
    } catch (error) {
      logger.error('Failed to send approval to backend:', error);
      return {
        approved: false,
        addToWhitelist: false,
        timedOut: false,
        error: 'Failed to communicate approval to backend'
      };
    }
  }

  /**
   * Handle deny action
   */
  private async handleDeny(approvalRequest: ToolApprovalRequest): Promise<ApprovalResult> {
    const { originalInteraction, toolName } = approvalRequest;

    try {
      // Send denial to backend
      await this.sendApprovalToBackend(originalInteraction, false, []);

      logger.info(`Tool denied: ${toolName}`);

      return {
        approved: false,
        addToWhitelist: false,
        timedOut: false
      };
    } catch (error) {
      logger.error('Failed to send denial to backend:', error);
      return {
        approved: false,
        addToWhitelist: false,
        timedOut: false,
        error: 'Failed to communicate denial to backend'
      };
    }
  }

  /**
   * Handle timeout
   */
  private handleTimeout(approvalId: string, resolve: (result: ApprovalResult) => void): void {
    const approvalRequest = this.activeApprovals.get(approvalId);
    
    if (!approvalRequest) {
      return; // Already handled
    }

    logger.info(`Tool approval timed out: ${approvalRequest.toolName} for user ${approvalRequest.userId}`);

    // Send timeout denial to backend
    this.sendApprovalToBackend(approvalRequest.originalInteraction, false, [])
      .catch(error => {
        logger.error('Failed to send timeout denial to backend:', error);
      });

    // Update the message to show timeout
    this.updateTimeoutMessage(approvalRequest).catch(error => {
      logger.error('Failed to update timeout message:', error);
    });

    // Resolve with timeout result
    resolve({
      approved: false,
      addToWhitelist: false,
      timedOut: true
    });

    // Clean up
    this.cleanupApproval(approvalId);
  }

  /**
   * Send approval decision to backend
   */
  private async sendApprovalToBackend(
    interaction: ChatInputCommandInteraction,
    approved: boolean,
    toolWhitelistUpdate: string[]
  ): Promise<void> {
    // Extract conversation context
    const context = this.conversationManager.extractConversationContext(interaction);
    
    // Build resume conversation payload with approval decision
    const payload: ResumeConversationPayload = {
      serverId: context.serverId,
      channelId: context.channelId,
      memberId: context.memberId,
      approved,
      toolWhitelistUpdate: toolWhitelistUpdate.length > 0 ? toolWhitelistUpdate : undefined
    };

    // Send to backend via resume conversation endpoint
    const responseStream = this.apiClient.resumeConversation(payload);
    
    // We don't need to handle the stream here as the original streaming handler will continue
    // Just consume the first response to ensure the approval was sent
    const firstResponse = await responseStream.next();
    if (firstResponse.done) {
      throw new Error('No response received from backend for approval');
    }
  }

  /**
   * Update approval message with result
   */
  private async updateApprovalMessage(
    interaction: ButtonInteraction,
    approvalRequest: ToolApprovalRequest,
    result: ApprovalResult
  ): Promise<void> {
    let color: number;
    let title: string;
    let description: string;

    if (result.error) {
      color = 0xFF0000; // Red
      title = '❌ Approval Failed';
      description = `Failed to process approval for **${approvalRequest.toolName}**: ${result.error}`;
    } else if (result.approved) {
      color = 0x00FF00; // Green
      title = result.addToWhitelist ? '🔒 Tool Approved & Trusted' : '✅ Tool Approved';
      description = result.addToWhitelist 
        ? `**${approvalRequest.toolName}** has been approved and added to your whitelist for automatic future approval.`
        : `**${approvalRequest.toolName}** has been approved for this request.`;
    } else {
      color = 0xFF0000; // Red
      title = '❌ Tool Denied';
      description = `**${approvalRequest.toolName}** has been denied.`;
    }

    const embed = {
      color,
      title,
      description,
      fields: [
        {
          name: 'Tool',
          value: approvalRequest.toolName,
          inline: true
        },
        {
          name: 'Action Taken',
          value: new Date().toLocaleString(),
          inline: true
        }
      ]
    };

    try {
      await interaction.editReply({
        content: '',
        embeds: [embed],
        components: [] // Remove buttons
      });
    } catch (error) {
      logger.error('Failed to update approval message:', error);
    }
  }

  /**
   * Update message when timeout occurs
   */
  private async updateTimeoutMessage(approvalRequest: ToolApprovalRequest): Promise<void> {
    const embed = {
      color: 0x808080, // Gray
      title: '⏰ Tool Approval Timed Out',
      description: `The approval request for **${approvalRequest.toolName}** has timed out and was automatically denied.`,
      fields: [
        {
          name: 'Tool',
          value: approvalRequest.toolName,
          inline: true
        },
        {
          name: 'Timed Out',
          value: new Date().toLocaleString(),
          inline: true
        }
      ]
    };

    try {
      if (approvalRequest.originalInteraction.replied || approvalRequest.originalInteraction.deferred) {
        await approvalRequest.originalInteraction.editReply({
          content: '',
          embeds: [embed],
          components: [] // Remove buttons
        });
      }
    } catch (error) {
      logger.error('Failed to update timeout message:', error);
    }
  }

  /**
   * Generate unique approval ID
   */
  private generateApprovalId(userId: string, toolName: string): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    return `${userId}_${toolName}_${timestamp}_${random}`;
  }

  /**
   * Clean up approval request
   */
  private cleanupApproval(approvalId: string): void {
    const approvalRequest = this.activeApprovals.get(approvalId);
    
    if (approvalRequest) {
      // Clear timeout
      clearTimeout(approvalRequest.timeoutId);
      
      // Remove from active approvals
      this.activeApprovals.delete(approvalId);
      
      logger.debug(`Cleaned up approval request: ${approvalId}`);
    }
  }

  /**
   * Clean up all expired approvals (called periodically)
   */
  public cleanupExpiredApprovals(): void {
    const now = Date.now();
    const expiredIds: string[] = [];

    for (const [approvalId, request] of this.activeApprovals.entries()) {
      // Check if approval is older than timeout + grace period
      const age = now - parseInt(approvalId.split('_')[2]);
      if (age > (this.config.approvalTimeoutSeconds + 30) * 1000) {
        expiredIds.push(approvalId);
      }
    }

    for (const approvalId of expiredIds) {
      this.cleanupApproval(approvalId);
    }

    if (expiredIds.length > 0) {
      logger.info(`Cleaned up ${expiredIds.length} expired approval requests`);
    }
  }
}