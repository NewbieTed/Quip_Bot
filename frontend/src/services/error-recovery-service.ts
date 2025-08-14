import { ChatInputCommandInteraction, Message } from 'discord.js';
import { AgentError, ERROR_MESSAGES, getUserFriendlyErrorMessage } from '../errors/agent-error';
import { logger } from '../utils/logger';

/**
 * Recovery strategy for different error scenarios
 */
interface RecoveryStrategy {
  canRecover: boolean;
  suggestedAction?: string;
  retryDelay?: number;
  fallbackMessage?: string;
}

/**
 * Error context for recovery decisions
 */
interface ErrorContext {
  operation: string;
  userId: string;
  username?: string;
  channelId?: string;
  guildId?: string;
  attempt?: number;
  maxAttempts?: number;
  [key: string]: any;
}

/**
 * Service for handling error recovery and user communication
 */
export class ErrorRecoveryService {
  private static instance: ErrorRecoveryService;
  private recoveryAttempts: Map<string, number> = new Map();
  private readonly maxRecoveryAttempts = 3;

  private constructor() {}

  public static getInstance(): ErrorRecoveryService {
    if (!ErrorRecoveryService.instance) {
      ErrorRecoveryService.instance = new ErrorRecoveryService();
    }
    return ErrorRecoveryService.instance;
  }

  /**
   * Handle error with automatic recovery strategy determination
   */
  async handleError(
    error: Error | AgentError,
    interaction: ChatInputCommandInteraction,
    context: ErrorContext
  ): Promise<void> {
    const agentError = this.ensureAgentError(error, context);
    const recoveryKey = this.getRecoveryKey(context);
    const currentAttempts = this.recoveryAttempts.get(recoveryKey) || 0;

    // Log error with privacy protection
    logger.error(`Error in ${context.operation}`, agentError, {
      userId: this.hashUserId(context.userId),
      username: context.username ? this.sanitizeUsername(context.username) : undefined,
      channelId: context.channelId ? this.hashId(context.channelId) : undefined,
      guildId: context.guildId ? this.hashId(context.guildId) : undefined,
      attempt: currentAttempts + 1,
      operation: context.operation
    });

    // Determine recovery strategy
    const strategy = this.determineRecoveryStrategy(agentError, currentAttempts, context);

    // Update attempt counter
    if (strategy.canRecover) {
      this.recoveryAttempts.set(recoveryKey, currentAttempts + 1);
    } else {
      this.recoveryAttempts.delete(recoveryKey);
    }

    // Send error message to user
    await this.sendErrorMessage(interaction, agentError, strategy, context);

    // Clean up old recovery attempts
    this.cleanupOldAttempts();
  }

  /**
   * Handle streaming errors with partial content preservation
   */
  async handleStreamingError(
    error: Error | AgentError,
    interaction: ChatInputCommandInteraction,
    partialContent?: string,
    context?: ErrorContext
  ): Promise<void> {
    const agentError = this.ensureAgentError(error, context);
    const errorMessage = getUserFriendlyErrorMessage(agentError);

    let finalContent: string;

    if (partialContent && partialContent.trim()) {
      const sanitizedPartial = partialContent.trim();
      
      if (agentError.type === 'stream_interrupted') {
        finalContent = `${sanitizedPartial}\n\n---\n⚠️ ${ERROR_MESSAGES.PARTIAL_RESPONSE}\n${errorMessage}`;
      } else if (agentError.recoverable) {
        finalContent = `${sanitizedPartial}\n\n---\n🔄 ${ERROR_MESSAGES.CONNECTION_LOST}\n${errorMessage}`;
      } else {
        finalContent = `${sanitizedPartial}\n\n---\n❌ ${ERROR_MESSAGES.STREAM_INTERRUPTED}\n${errorMessage}`;
      }

      // Add recovery suggestions
      if (agentError.recoverable) {
        finalContent += '\n\n🔄 You can try your request again to continue.';
      }
    } else {
      if (agentError.recoverable) {
        finalContent = `🔄 ${errorMessage}\n\nYou can try your request again.`;
      } else {
        finalContent = `❌ ${errorMessage}`;
      }
    }

    await this.safeReply(interaction, finalContent);

    // Log streaming error details
    logger.error('Streaming error handled', agentError, {
      hasPartialContent: !!partialContent,
      partialContentLength: partialContent?.length || 0,
      errorRecoverable: agentError.recoverable,
      operation: context?.operation || 'streaming'
    });
  }

  /**
   * Determine recovery strategy based on error type and context
   */
  private determineRecoveryStrategy(
    error: AgentError,
    currentAttempts: number,
    context: ErrorContext
  ): RecoveryStrategy {
    // Check if we've exceeded max recovery attempts
    if (currentAttempts >= this.maxRecoveryAttempts) {
      return {
        canRecover: false,
        fallbackMessage: ERROR_MESSAGES.RETRY_EXHAUSTED
      };
    }

    switch (error.type) {
      case 'network':
      case 'timeout':
        return {
          canRecover: true,
          suggestedAction: 'This appears to be a temporary network issue. Please try again.',
          retryDelay: Math.min(1000 * Math.pow(2, currentAttempts), 10000) // Exponential backoff
        };

      case 'rate_limit':
        return {
          canRecover: true,
          suggestedAction: 'Rate limit reached. Please wait a moment before trying again.',
          retryDelay: 30000 // 30 seconds for rate limits
        };

      case 'stream_interrupted':
        return {
          canRecover: true,
          suggestedAction: 'Connection was interrupted. You can try your request again.'
        };

      case 'api':
        if (error.message.includes('404') || error.message.includes('not found')) {
          return {
            canRecover: false,
            suggestedAction: 'Resource not found. For conversations, try using `/lenza-new` to start fresh.'
          };
        } else if (error.message.includes('500') || error.message.includes('502') || error.message.includes('503')) {
          return {
            canRecover: true,
            suggestedAction: 'Server is experiencing issues. Please try again in a moment.',
            retryDelay: 5000
          };
        }
        return {
          canRecover: false,
          suggestedAction: 'API error occurred. Please try again or contact support if the issue persists.'
        };

      case 'validation':
        return {
          canRecover: false,
          suggestedAction: 'Please check your input and try again.'
        };

      case 'permission':
        return {
          canRecover: false,
          suggestedAction: 'You don\'t have permission for this action. Contact a server administrator if needed.'
        };

      case 'configuration':
        return {
          canRecover: false,
          suggestedAction: 'Bot configuration issue. Please contact support.',
          fallbackMessage: ERROR_MESSAGES.CONFIGURATION_ERROR
        };

      default:
        return {
          canRecover: currentAttempts < 2, // Allow one retry for unknown errors
          suggestedAction: 'An unexpected error occurred. Please try again.'
        };
    }
  }

  /**
   * Send error message to user with recovery guidance
   */
  private async sendErrorMessage(
    interaction: ChatInputCommandInteraction,
    error: AgentError,
    strategy: RecoveryStrategy,
    context: ErrorContext
  ): Promise<void> {
    let errorMessage = strategy.fallbackMessage || getUserFriendlyErrorMessage(error);

    // Add specific guidance based on error type and strategy
    if (strategy.suggestedAction) {
      errorMessage += `\n\n💡 **Suggestion**: ${strategy.suggestedAction}`;
    }

    // Add retry information if recoverable
    if (strategy.canRecover) {
      const currentAttempts = this.recoveryAttempts.get(this.getRecoveryKey(context)) || 0;
      const remainingAttempts = this.maxRecoveryAttempts - currentAttempts;
      
      if (remainingAttempts > 0) {
        errorMessage += `\n\n🔄 You can try again (${remainingAttempts} attempts remaining).`;
      }

      if (strategy.retryDelay && strategy.retryDelay > 5000) {
        const seconds = Math.ceil(strategy.retryDelay / 1000);
        errorMessage += `\n⏱️ Please wait ${seconds} seconds before retrying.`;
      }
    }

    // Add context-specific help
    if (context.operation === 'lenza-resume' && error.type === 'api' && error.message.includes('404')) {
      errorMessage += '\n\n🆕 **Tip**: Use `/lenza-new` to start a fresh conversation.';
    }

    await this.safeReply(interaction, `❌ ${errorMessage}`);
  }

  /**
   * Safely reply to interaction with fallback handling
   */
  private async safeReply(interaction: ChatInputCommandInteraction, content: string): Promise<void> {
    try {
      if (interaction.replied || interaction.deferred) {
        await interaction.editReply(content);
      } else {
        await interaction.reply(content);
      }
    } catch (replyError) {
      logger.critical('Failed to send error message to user', replyError as Error, {
        userId: this.hashUserId(interaction.user.id),
        originalContent: content.substring(0, 100) // Truncate for logging
      });

      // Last resort: try a simple fallback message
      try {
        const fallbackMessage = '❌ An error occurred and I was unable to provide details. Please try again.';
        if (interaction.replied || interaction.deferred) {
          await interaction.editReply(fallbackMessage);
        } else {
          await interaction.reply(fallbackMessage);
        }
      } catch (finalError) {
        logger.critical('Complete failure to communicate with user', finalError as Error, {
          userId: this.hashUserId(interaction.user.id)
        });
      }
    }
  }

  /**
   * Ensure error is an AgentError instance
   */
  private ensureAgentError(error: Error | AgentError, context?: ErrorContext): AgentError {
    if (error instanceof AgentError) {
      return error;
    }

    // Convert regular Error to AgentError
    if (error instanceof Error) {
      if (error.name === 'AbortError' || error.message.includes('timeout')) {
        return new AgentError('timeout', 'Request timed out', error, context);
      } else if (error.message.includes('network') || error.message.includes('fetch')) {
        return new AgentError('network', 'Network error occurred', error, context);
      } else if (error.message.includes('validation') || error.message.includes('invalid')) {
        return new AgentError('validation', error.message, error, context);
      }
    }

    return new AgentError('unknown', 'An unknown error occurred', error, context);
  }

  /**
   * Generate recovery key for tracking attempts
   */
  private getRecoveryKey(context: ErrorContext): string {
    return `${context.operation}_${context.userId}_${context.channelId || 'dm'}`;
  }

  /**
   * Clean up old recovery attempts (older than 1 hour)
   */
  private cleanupOldAttempts(): void {
    // This is a simple implementation - in production you might want to track timestamps
    if (this.recoveryAttempts.size > 1000) {
      // Clear half of the entries if we have too many
      const entries = Array.from(this.recoveryAttempts.entries());
      const toKeep = entries.slice(entries.length / 2);
      this.recoveryAttempts.clear();
      toKeep.forEach(([key, value]) => this.recoveryAttempts.set(key, value));
    }
  }

  /**
   * Get recovery statistics for monitoring
   */
  public getRecoveryStats(): { totalAttempts: number; activeRecoveries: number } {
    const totalAttempts = Array.from(this.recoveryAttempts.values()).reduce((sum, attempts) => sum + attempts, 0);
    return {
      totalAttempts,
      activeRecoveries: this.recoveryAttempts.size
    };
  }

  /**
   * Reset recovery attempts for a specific context (for testing or admin operations)
   */
  public resetRecoveryAttempts(context: ErrorContext): void {
    const key = this.getRecoveryKey(context);
    this.recoveryAttempts.delete(key);
  }

  // Privacy helper methods (these would ideally be shared utilities)
  private hashUserId(userId: string): string {
    return `user_${userId.slice(-4)}`;
  }

  private hashId(id: string): string {
    return `id_${id.slice(-4)}`;
  }

  private sanitizeUsername(username: string): string {
    if (username.length <= 2) return '**';
    return username[0] + '*'.repeat(username.length - 2) + username[username.length - 1];
  }
}

// Export singleton instance
export const errorRecoveryService = ErrorRecoveryService.getInstance();