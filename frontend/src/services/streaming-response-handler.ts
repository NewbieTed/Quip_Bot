import { ChatInputCommandInteraction, Message } from 'discord.js';
import { StreamingResponse } from '../models/agent-models';
import { getBotConfig } from '../config/bot-config';
import { AgentError, ERROR_MESSAGES } from '../errors/agent-error';
import { ToolApprovalHandler } from './tool-approval-handler';
import { errorRecoveryService } from './error-recovery-service';
import { logger } from '../utils/logger';

/**
 * Handles streaming responses from the agent and updates Discord messages in real-time
 */
export class StreamingResponseHandler {
  private config = getBotConfig();
  private interaction: ChatInputCommandInteraction;
  private currentMessage: Message | null = null;
  private currentContent: string = '';
  private messageCount: number = 0;
  private isStreamActive: boolean = false;
  private lastUpdateTime: number = 0;
  private readonly UPDATE_THROTTLE_MS = 500; // Throttle updates to avoid rate limits
  private toolApprovalHandler: ToolApprovalHandler;

  constructor(interaction: ChatInputCommandInteraction) {
    this.interaction = interaction;
    this.toolApprovalHandler = new ToolApprovalHandler();
  }

  /**
   * Handle streaming response from agent with enhanced error recovery
   */
  async handleStream(responseStream: AsyncGenerator<StreamingResponse>): Promise<void> {
    this.isStreamActive = true;
    let hasContent = false;
    let lastProgressMessage = '';
    let streamInterrupted = false;
    let partialContent = '';
    const startTime = Date.now();

    try {
      logger.info('Starting stream processing');

      for await (const response of responseStream) {
        if (!this.isStreamActive) {
          logger.info('Stream was cancelled by user, stopping processing');
          streamInterrupted = true;
          break;
        }

        if (response.content && response.content.trim()) {
          hasContent = true;
          partialContent = this.currentContent; // Save partial content before processing
          await this.processResponse(response);
        }
      }

      // Handle stream completion
      if (streamInterrupted) {
        await this.handleStreamInterruption(partialContent, 'User cancelled');
      } else {
        // Clear any lingering progress indicators
        if (lastProgressMessage && this.currentContent) {
          await this.finalizeMessage();
        }

        if (!hasContent) {
          await this.handleEmptyResponse();
        } else {
          const duration = Date.now() - startTime;
          logger.performance('stream_processing', duration, {
            contentLength: this.currentContent.length,
            messageCount: this.messageCount
          });
        }
      }

      logger.info('Streaming response handling completed successfully');
    } catch (error) {
      logger.error('Error handling streaming response:', error);
      
      // Save partial content before handling error
      partialContent = this.currentContent;
      
      // Use error recovery service for streaming errors
      await errorRecoveryService.handleStreamingError(
        error as Error,
        this.interaction,
        partialContent,
        {
          operation: 'streaming_response',
          userId: this.interaction.user.id,
          username: this.interaction.user.username,
          channelId: this.interaction.channel?.id,
          guildId: this.interaction.guild?.id,
          contentLength: partialContent.length,
          messageCount: this.messageCount
        }
      );
    } finally {
      this.isStreamActive = false;
    }
  }

  /**
   * Process individual streaming response
   */
  private async processResponse(response: StreamingResponse): Promise<void> {
    const now = Date.now();
    
    switch (response.type) {
      case 'progress':
        await this.handleProgressResponse(response);
        break;
      case 'update':
        await this.handleUpdateResponse(response);
        break;
      case 'interrupt':
        await this.handleInterruptResponse(response);
        break;
      default:
        // Treat unknown as update but log for debugging
        logger.warn(`Unknown response type: ${response.type}, treating as update`);
        await this.handleUpdateResponse(response);
    }
    
    this.lastUpdateTime = now;
  }

  /**
   * Handle progress response (typically shows loading indicators)
   */
  private async handleProgressResponse(response: StreamingResponse): Promise<void> {
    const progressContent = this.formatProgressMessage(response.content, response.tool_name);
    
    // Show progress immediately, but throttle if we have existing content
    if (!this.currentContent || this.shouldUpdateNow()) {
      await this.updateMessage(progressContent);
    }
  }

  /**
   * Handle update response (accumulates content)
   */
  private async handleUpdateResponse(response: StreamingResponse): Promise<void> {
    this.currentContent += response.content;
    
    // Throttle updates to avoid Discord rate limits
    if (this.shouldUpdateNow()) {
      await this.updateMessage(this.currentContent);
    }
  }

  /**
   * Handle interrupt response (tool approval requests, etc.)
   */
  private async handleInterruptResponse(response: StreamingResponse): Promise<void> {
    // Check if this is a tool approval request
    if (response.tool_name && this.isToolApprovalRequest(response.content)) {
      await this.handleToolApprovalRequest(response);
    } else {
      // For other interrupts, show the content immediately
      const interruptContent = this.formatInterruptMessage(response.content, response.tool_name);
      await this.updateMessage(interruptContent);
    }
  }

  /**
   * Check if the response content indicates a tool approval request
   */
  private isToolApprovalRequest(content: string): boolean {
    // Look for common patterns that indicate tool approval is needed
    const approvalPatterns = [
      /approval.*required/i,
      /permission.*needed/i,
      /authorize.*tool/i,
      /approve.*use/i,
      /tool.*approval/i
    ];

    return approvalPatterns.some(pattern => pattern.test(content));
  }

  /**
   * Handle tool approval request
   */
  private async handleToolApprovalRequest(response: StreamingResponse): Promise<void> {
    if (!response.tool_name) {
      logger.warn('Tool approval request received without tool name');
      return;
    }

    try {
      logger.info(`Handling tool approval request for: ${response.tool_name}`);

      // Request approval from user
      const approvalResult = await this.toolApprovalHandler.requestApproval(
        this.interaction,
        response.tool_name,
        response.content
      );

      logger.info(`Tool approval result: approved=${approvalResult.approved}, addToWhitelist=${approvalResult.addToWhitelist}, timedOut=${approvalResult.timedOut}`);

      // The approval handler already communicates the result to the backend
      // The stream will continue with the backend's response

    } catch (error) {
      logger.error('Error handling tool approval request:', error);
      
      // Show error message to user
      const errorContent = `❌ Failed to process tool approval request for **${response.tool_name}**: ${error instanceof Error ? error.message : 'Unknown error'}`;
      await this.updateMessage(errorContent);
    }
  }

  /**
   * Update Discord message with new content
   */
  private async updateMessage(content: string): Promise<void> {
    try {
      // Check if content exceeds Discord limits and needs splitting
      if (content.length > this.config.maxMessageLength) {
        await this.splitLongMessage(content);
        return;
      }

      const formattedContent = this.formatMessageContent(content);
      
      if (this.currentMessage) {
        // Edit existing message
        await this.currentMessage.edit(formattedContent);
      } else {
        // Create initial reply
        if (this.interaction.replied || this.interaction.deferred) {
          this.currentMessage = await this.interaction.editReply(formattedContent);
        } else {
          this.currentMessage = await this.interaction.reply({
            content: formattedContent,
            fetchReply: true
          }) as Message;
        }
      }
    } catch (error) {
      logger.error('Failed to update Discord message:', error);
      
      // If we hit rate limits, try to recover gracefully
      if (error instanceof Error && error.message?.includes('rate limit')) {
        logger.warn('Hit Discord rate limit, throttling updates');
        await this.sleep(2000); // Wait 2 seconds before continuing
      }
      
      // Don't throw here to avoid breaking the stream
    }
  }

  /**
   * Handle streaming errors with enhanced recovery and partial content preservation
   */
  private async handleStreamError(error: AgentError, partialContent?: string): Promise<void> {
    const errorMessage = this.getErrorMessage(error);
    let finalContent: string;
    
    if (partialContent && partialContent.trim()) {
      // Show partial content with error indication
      const sanitizedPartial = partialContent.trim();
      
      if (error.type === 'stream_interrupted') {
        finalContent = `${sanitizedPartial}\n\n---\n⚠️ ${ERROR_MESSAGES.PARTIAL_RESPONSE}\n${errorMessage}`;
      } else if (error.recoverable) {
        finalContent = `${sanitizedPartial}\n\n---\n🔄 ${ERROR_MESSAGES.CONNECTION_LOST}\n${errorMessage}`;
      } else {
        finalContent = `${sanitizedPartial}\n\n---\n❌ ${ERROR_MESSAGES.STREAM_INTERRUPTED}\n${errorMessage}`;
      }
    } else {
      // No partial content available
      if (error.recoverable) {
        finalContent = `🔄 ${errorMessage}\n\nYou can try your request again.`;
      } else {
        finalContent = `❌ ${errorMessage}`;
      }
    }
    
    await this.updateMessage(finalContent);
    
    // Log error details for debugging
    logger.error('Stream error handled', error, {
      hasPartialContent: !!partialContent,
      partialContentLength: partialContent?.length || 0,
      errorRecoverable: error.recoverable
    });
  }

  /**
   * Handle stream interruption (user cancellation, etc.)
   */
  private async handleStreamInterruption(partialContent: string, reason: string): Promise<void> {
    let finalContent: string;
    
    if (partialContent && partialContent.trim()) {
      finalContent = `${partialContent.trim()}\n\n---\n⏹️ Stream stopped: ${reason}`;
    } else {
      finalContent = `⏹️ Stream stopped: ${reason}`;
    }
    
    await this.updateMessage(finalContent);
    
    logger.info('Stream interruption handled', {
      reason,
      hasPartialContent: !!partialContent,
      partialContentLength: partialContent?.length || 0
    });
  }

  /**
   * Handle empty response scenario
   */
  private async handleEmptyResponse(): Promise<void> {
    const message = "🤔 I didn't receive any response from Lenza. This might be due to:\n\n" +
                   "• Network connectivity issues\n" +
                   "• Service temporarily unavailable\n" +
                   "• Request processing timeout\n\n" +
                   "Please try your request again in a moment.";
    
    await this.updateMessage(message);
    
    logger.warn('Empty response received from stream', {
      streamDuration: Date.now() - (this.lastUpdateTime || Date.now())
    });
  }

  /**
   * Format message content with proper Discord formatting
   */
  private formatMessageContent(content: string): string {
    const trimmedContent = content.trim();
    
    if (!trimmedContent) {
      return '⏳ Processing...';
    }
    
    // Apply basic formatting improvements
    let formatted = trimmedContent;
    
    // Ensure code blocks are properly formatted
    formatted = this.ensureProperCodeBlocks(formatted);
    
    return formatted;
  }

  /**
   * Split long content into multiple messages
   */
  private async splitLongMessage(content: string): Promise<void> {
    const maxChunkSize = this.config.maxMessageLength - 100; // Leave room for continuation indicators
    const chunks = this.splitIntoChunks(content, maxChunkSize);
    
    logger.info(`Splitting long message into ${chunks.length} chunks`);
    
    for (let i = 0; i < chunks.length; i++) {
      const chunk = chunks[i];
      const isFirst = i === 0;
      const isLast = i === chunks.length - 1;
      
      let formattedChunk = chunk.trim();
      
      // Add continuation indicators
      if (!isFirst) {
        formattedChunk = `📄 *(Continued from previous message...)*\n\n${formattedChunk}`;
      }
      
      if (!isLast) {
        formattedChunk = `${formattedChunk}\n\n📄 *(Continued in next message...)*`;
      }
      
      try {
        if (isFirst && this.currentMessage) {
          // Update the existing message with the first chunk
          await this.currentMessage.edit(formattedChunk);
        } else {
          // Send follow-up messages for additional chunks
          await this.interaction.followUp(formattedChunk);
          this.messageCount++;
        }
        
        // Small delay between messages to avoid rate limits
        if (!isLast) {
          await this.sleep(500);
        }
      } catch (error) {
        logger.error(`Failed to send message chunk ${i + 1}:`, error);
        
        // If we can't send more chunks, indicate truncation
        if (i === 0) {
          const truncatedContent = content.substring(0, maxChunkSize - 100);
          await this.updateMessage(`${truncatedContent}\n\n⚠️ *(Response truncated due to Discord limits)*`);
        }
        break;
      }
    }
  }

  /**
   * Split content into chunks that fit Discord message limits
   * Preserves code blocks and formatting when possible
   */
  private splitIntoChunks(content: string, maxLength: number): string[] {
    const chunks: string[] = [];
    let currentChunk = '';
    
    // Try to split by paragraphs first (double newlines)
    const paragraphs = content.split('\n\n');
    
    for (const paragraph of paragraphs) {
      const paragraphWithNewlines = paragraph + '\n\n';
      
      if (currentChunk.length + paragraphWithNewlines.length <= maxLength) {
        currentChunk += paragraphWithNewlines;
      } else {
        // Current chunk is full, save it and start new one
        if (currentChunk.trim()) {
          chunks.push(currentChunk.trim());
        }
        
        // If single paragraph is too long, split by lines
        if (paragraphWithNewlines.length > maxLength) {
          const lines = paragraph.split('\n');
          let lineChunk = '';
          
          for (const line of lines) {
            if (lineChunk.length + line.length + 1 <= maxLength) {
              lineChunk += (lineChunk ? '\n' : '') + line;
            } else {
              if (lineChunk.trim()) {
                chunks.push(lineChunk.trim());
              }
              
              // If single line is still too long, force split
              if (line.length > maxLength) {
                let remainingLine = line;
                while (remainingLine.length > maxLength) {
                  chunks.push(remainingLine.substring(0, maxLength));
                  remainingLine = remainingLine.substring(maxLength);
                }
                lineChunk = remainingLine;
              } else {
                lineChunk = line;
              }
            }
          }
          
          currentChunk = lineChunk ? lineChunk + '\n\n' : '';
        } else {
          currentChunk = paragraphWithNewlines;
        }
      }
    }
    
    if (currentChunk.trim()) {
      chunks.push(currentChunk.trim());
    }
    
    return chunks;
  }

  /**
   * Format progress message with indicators
   */
  private formatProgressMessage(content: string, toolName?: string): string {
    const indicator = this.getProgressIndicator();
    const toolInfo = toolName ? ` (${toolName})` : '';
    return `${indicator} ${content}${toolInfo}`;
  }

  /**
   * Format interrupt message (for tool approvals, etc.)
   */
  private formatInterruptMessage(content: string, toolName?: string): string {
    const toolInfo = toolName ? ` **${toolName}**` : '';
    return `⚠️ ${content}${toolInfo}`;
  }

  /**
   * Get rotating progress indicator
   */
  private getProgressIndicator(): string {
    const indicators = ['⏳', '🔄', '⚡', '🤔'];
    const index = Math.floor(Date.now() / 1000) % indicators.length;
    return indicators[index];
  }

  /**
   * Check if we should update the message now (throttling)
   */
  private shouldUpdateNow(): boolean {
    const now = Date.now();
    return now - this.lastUpdateTime >= this.UPDATE_THROTTLE_MS;
  }

  /**
   * Finalize message by removing progress indicators
   */
  private async finalizeMessage(): Promise<void> {
    if (this.currentContent) {
      await this.updateMessage(this.currentContent);
    }
  }

  /**
   * Get appropriate error message based on error type
   */
  private getErrorMessage(error: AgentError): string {
    switch (error.type) {
      case 'timeout':
        return ERROR_MESSAGES.NETWORK_TIMEOUT;
      case 'network':
        return ERROR_MESSAGES.AGENT_UNAVAILABLE;
      case 'parsing':
        return ERROR_MESSAGES.PARSING_ERROR;
      case 'api':
        return error.message.includes('rate limit') 
          ? ERROR_MESSAGES.RATE_LIMITED 
          : ERROR_MESSAGES.AGENT_UNAVAILABLE;
      default:
        return ERROR_MESSAGES.UNKNOWN_ERROR;
    }
  }

  /**
   * Ensure code blocks are properly formatted for Discord
   */
  private ensureProperCodeBlocks(content: string): string {
    // Fix common code block formatting issues
    let formatted = content;
    
    // Ensure code blocks have proper spacing
    formatted = formatted.replace(/```(\w+)?\n/g, '```$1\n');
    formatted = formatted.replace(/\n```/g, '\n```');
    
    return formatted;
  }

  /**
   * Sleep utility for throttling
   */
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Cancel the current stream
   */
  public cancelStream(): void {
    this.isStreamActive = false;
    logger.info('Stream cancellation requested');
  }
}