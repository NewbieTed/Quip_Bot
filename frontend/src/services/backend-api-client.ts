import fetch, { Response } from 'node-fetch';
import { getBotConfig } from '../config/bot-config';
import { AgentError } from '../errors/agent-error';
import { NetworkErrorHandler } from '../utils/network-error-handler';
import { logger } from '../utils/logger';
import {
  BackendAgentRequest,
  StreamingResponse,
  ToolWhitelistRequest,
  ToolWhitelistResponse,
  BackendConversationRequest,
  NewConversationPayload,
  ResumeConversationPayload
} from '../models/agent-models';
import { ConversationManager } from './conversation-manager';

/**
 * Backend API client for communicating with Spring Boot backend
 */
export class BackendApiClient {
  private config = getBotConfig();
  private networkHandler = new NetworkErrorHandler();
  private conversationManager = new ConversationManager();
  private baseUrl: string;

  constructor() {
    this.baseUrl = this.config.backendApiUrl;
    logger.info(`Backend API client initialized with URL: ${this.baseUrl}`);
  }

  /**
   * Invoke agent for continuing existing conversation
   * Note: Legacy method, prefer resumeConversation
   */
  async *invokeAgent(request: BackendAgentRequest): AsyncGenerator<StreamingResponse> {
    const url = `${this.baseUrl}/assistant`;
    
    yield* this.streamAgentResponse(url, request, 'invoke');
  }

  /**
   * Invoke agent for new conversation using conversation management
   */
  async *invokeNewConversation(payload: NewConversationPayload): AsyncGenerator<StreamingResponse> {
    const backendRequest = this.conversationManager.convertToBackendRequest(payload);
    const url = `${this.baseUrl}/assistant/new`;
    
    logger.info(`Starting new conversation: serverId=${payload.serverId}, channelId=${payload.channelId}, memberId=${payload.memberId}`);
    
    yield* this.streamAgentResponse(url, backendRequest, 'new');
  }

  /**
   * Resume existing conversation using conversation management
   */
  async *resumeConversation(payload: ResumeConversationPayload): AsyncGenerator<StreamingResponse> {
    const backendRequest = this.conversationManager.convertToBackendRequest(payload);
    const url = `${this.baseUrl}/assistant`;
    
    logger.info(`Resuming conversation: serverId=${payload.serverId}, channelId=${payload.channelId}, memberId=${payload.memberId}, hasMessage=${payload.message !== undefined}, approved=${payload.approved}`);
    
    yield* this.streamAgentResponse(url, backendRequest, 'resume');
  }

  /**
   * Invoke agent for new conversation
   * Note: Legacy method, prefer invokeNewConversation
   */
  async *invokeNewAgent(request: BackendAgentRequest): AsyncGenerator<StreamingResponse> {
    const url = `${this.baseUrl}/assistant/new`;
    
    yield* this.streamAgentResponse(url, request, 'new');
  }

  /**
   * Send tool approval decision to backend
   */
  async sendApprovalDecision(request: BackendAgentRequest): Promise<void> {
    const url = `${this.baseUrl}/assistant/approve`;
    
    await this.networkHandler.handleWithRetry(async () => {
      const response = await this.makeRequest(url, request);
      
      if (!response.ok) {
        throw new AgentError('api', `Approval request failed: ${response.status} ${response.statusText}`);
      }
    }, 2); // Fewer retries for approval decisions
  }

  /**
   * Update tool whitelist
   */
  async updateToolWhitelist(request: ToolWhitelistRequest): Promise<ToolWhitelistResponse> {
    const url = `${this.baseUrl}/tool-whitelist/update`;
    
    return await this.networkHandler.handleWithRetry(async () => {
      const response = await this.makeRequest(url, request);
      
      if (!response.ok) {
        throw new AgentError('api', `Whitelist update failed: ${response.status} ${response.statusText}`);
      }

      const result = await response.json() as any;
      return {
        success: result.success || true,
        message: result.message
      };
    });
  }

  /**
   * Stream agent response from backend with enhanced error handling and recovery
   */
  private async *streamAgentResponse(
    url: string, 
    request: BackendAgentRequest | BackendConversationRequest, 
    type: 'invoke' | 'new' | 'resume'
  ): AsyncGenerator<StreamingResponse> {
    let response: Response | undefined;
    const startTime = Date.now();
    let bytesReceived = 0;
    let linesProcessed = 0;
    
    try {
      // Make request with enhanced context
      response = await this.networkHandler.handleWithRetry(async () => {
        return await this.makeRequest(url, request);
      }, this.config.retryAttempts, {
        operationName: `stream_${type}_agent`,
        url,
        requestType: type
      });

      if (!response.ok) {
        const error = this.createErrorFromResponse(response, url);
        throw error;
      }

      if (!response.body) {
        throw new AgentError('api', 'No response body received from backend', undefined, {
          url,
          status: response.status,
          headers: Object.fromEntries(response.headers.entries())
        });
      }

      logger.info(`Starting to stream ${type} agent response`, {
        url,
        status: response.status,
        contentType: response.headers.get('content-type')
      });

      // Process streaming response with enhanced error handling
      const reader = response.body;
      let buffer = '';
      let lastChunkTime = Date.now();
      const chunkTimeout = 30000; // 30 seconds timeout between chunks

      try {
        for await (const chunk of reader) {
          const now = Date.now();
          
          // Check for chunk timeout
          if (now - lastChunkTime > chunkTimeout) {
            logger.warn('Chunk timeout detected in stream', {
              timeSinceLastChunk: now - lastChunkTime,
              timeout: chunkTimeout
            });
            
            // Yield a progress update to keep user informed
            yield {
              content: 'Still processing... (connection slow)',
              type: 'progress'
            };
          }
          
          lastChunkTime = now;
          const chunkStr = chunk.toString();
          buffer += chunkStr;
          bytesReceived += chunkStr.length;

          // Process complete lines
          const lines = buffer.split('\n');
          buffer = lines.pop() || ''; // Keep incomplete line in buffer

          for (const line of lines) {
            if (line.trim()) {
              try {
                const parsed = this.parseStreamingResponse(line.trim());
                if (parsed) {
                  linesProcessed++;
                  yield parsed;
                }
              } catch (parseError) {
                logger.warn('Failed to parse streaming line', {
                  line: line.substring(0, 100), // Truncate for logging
                  lineNumber: linesProcessed + 1,
                  error: (parseError as Error).message
                });
                
                // Try to recover by treating as plain text
                if (line.trim().length > 0) {
                  yield {
                    content: line.trim(),
                    type: 'update'
                  };
                  linesProcessed++;
                }
              }
            }
          }
        }

        // Process any remaining buffer content
        if (buffer.trim()) {
          try {
            const parsed = this.parseStreamingResponse(buffer.trim());
            if (parsed) {
              yield parsed;
              linesProcessed++;
            }
          } catch (parseError) {
            logger.warn('Failed to parse final buffer', {
              buffer: buffer.substring(0, 100),
              error: (parseError as Error).message
            });
            
            // Treat as plain text if it looks like content
            if (buffer.trim().length > 0) {
              yield {
                content: buffer.trim(),
                type: 'update'
              };
              linesProcessed++;
            }
          }
        }

      } catch (streamError) {
        // Handle streaming-specific errors
        if (streamError instanceof Error) {
          if (streamError.name === 'AbortError') {
            throw new AgentError('timeout', 'Stream reading timed out', streamError, {
              url,
              bytesReceived,
              linesProcessed
            });
          } else if (streamError.message.includes('network') || streamError.message.includes('connection')) {
            throw new AgentError('stream_interrupted', 'Network connection lost during streaming', streamError, {
              url,
              bytesReceived,
              linesProcessed
            });
          }
        }
        
        throw new AgentError('stream_interrupted', 'Stream processing failed', streamError as Error, {
          url,
          bytesReceived,
          linesProcessed
        });
      }

      const duration = Date.now() - startTime;
      logger.performance(`stream_${type}_agent`, duration, {
        bytesReceived,
        linesProcessed,
        avgBytesPerSecond: Math.round(bytesReceived / (duration / 1000))
      });

      logger.info(`Completed streaming ${type} agent response`, {
        duration,
        bytesReceived,
        linesProcessed
      });

    } catch (error) {
      const duration = Date.now() - startTime;
      logger.error(`Error in streamAgentResponse for ${type}`, error as Error, {
        url,
        duration,
        bytesReceived,
        linesProcessed
      });
      
      if (error instanceof AgentError) {
        throw error;
      }
      
      throw new AgentError('network', 'Failed to stream agent response', error as Error, {
        url,
        type,
        duration,
        bytesReceived
      });
    }
  }

  /**
   * Create appropriate error from HTTP response
   */
  private createErrorFromResponse(response: Response, url: string): AgentError {
    const context = {
      url,
      status: response.status,
      statusText: response.statusText,
      headers: Object.fromEntries(response.headers.entries())
    };

    switch (response.status) {
      case 400:
        return new AgentError('validation', `Bad request: ${response.statusText}`, undefined, context);
      case 401:
      case 403:
        return new AgentError('permission', `Access denied: ${response.statusText}`, undefined, context);
      case 404:
        return new AgentError('api', `Resource not found: ${response.statusText}`, undefined, context);
      case 429:
        return new AgentError('rate_limit', `Rate limit exceeded: ${response.statusText}`, undefined, context);
      case 500:
      case 502:
      case 503:
      case 504:
        return new AgentError('api', `Server error: ${response.statusText}`, undefined, context);
      default:
        return new AgentError('api', `HTTP error ${response.status}: ${response.statusText}`, undefined, context);
    }
  }

  /**
   * Parse streaming response line
   */
  private parseStreamingResponse(line: string): StreamingResponse | null {
    if (!line || line.trim() === '') {
      return null;
    }

    try {
      // Try to parse as JSON first
      const parsed = JSON.parse(line);
      
      return {
        content: parsed.content || '',
        type: this.normalizeResponseType(parsed.type),
        tool_name: parsed.tool_name
      };
    } catch (error) {
      // If not JSON, treat as plain text content
      return {
        content: line,
        type: 'update'
      };
    }
  }

  /**
   * Normalize response type to expected values
   */
  private normalizeResponseType(type: string): StreamingResponse['type'] {
    switch (type?.toLowerCase()) {
      case 'progress':
        return 'progress';
      case 'update':
        return 'update';
      case 'interrupt':
        return 'interrupt';
      default:
        return 'unknown';
    }
  }

  /**
   * Make HTTP request with timeout and proper headers
   */
  private async makeRequest(url: string, body: any): Promise<Response> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.config.backendApiTimeout);

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/plain'
        },
        body: JSON.stringify(body),
        signal: controller.signal
      });

      return response;
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        throw new AgentError('timeout', `Request to ${url} timed out after ${this.config.backendApiTimeout}ms`);
      }
      throw error;
    } finally {
      clearTimeout(timeoutId);
    }
  }
}