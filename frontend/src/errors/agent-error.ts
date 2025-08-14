/**
 * Agent error types for categorizing different error scenarios
 */
export type AgentErrorType = 
  | 'network' 
  | 'timeout' 
  | 'parsing' 
  | 'api' 
  | 'rate_limit'
  | 'validation'
  | 'permission'
  | 'stream_interrupted'
  | 'discord_api'
  | 'configuration'
  | 'unknown';

/**
 * Error severity levels for logging and handling
 */
export type ErrorSeverity = 'low' | 'medium' | 'high' | 'critical';

/**
 * Custom error class for agent-related errors
 */
export class AgentError extends Error {
  public readonly type: AgentErrorType;
  public readonly severity: ErrorSeverity;
  public readonly originalError?: Error;
  public readonly context?: Record<string, any>;
  public readonly timestamp: Date;
  public readonly recoverable: boolean;

  constructor(
    type: AgentErrorType,
    message: string,
    originalError?: Error,
    context?: Record<string, any>,
    severity?: ErrorSeverity
  ) {
    super(message);
    this.name = 'AgentError';
    this.type = type;
    this.severity = severity || this.determineSeverity(type);
    this.originalError = originalError;
    this.context = context;
    this.timestamp = new Date();
    this.recoverable = this.determineRecoverability(type);
  }

  /**
   * Determine error severity based on type
   */
  private determineSeverity(type: AgentErrorType): ErrorSeverity {
    switch (type) {
      case 'network':
      case 'timeout':
      case 'rate_limit':
        return 'medium';
      case 'stream_interrupted':
      case 'discord_api':
        return 'low';
      case 'api':
      case 'parsing':
        return 'high';
      case 'configuration':
      case 'permission':
        return 'critical';
      default:
        return 'medium';
    }
  }

  /**
   * Determine if error is recoverable
   */
  private determineRecoverability(type: AgentErrorType): boolean {
    switch (type) {
      case 'network':
      case 'timeout':
      case 'rate_limit':
      case 'stream_interrupted':
        return true;
      case 'validation':
      case 'permission':
      case 'configuration':
        return false;
      default:
        return false;
    }
  }

  /**
   * Get sanitized error for user display (removes sensitive information)
   */
  public getSanitizedMessage(): string {
    // Remove potentially sensitive information from error messages
    let sanitized = this.message;
    
    // Remove URLs, IPs, and other sensitive patterns
    sanitized = sanitized.replace(/https?:\/\/[^\s]+/g, '[URL]');
    sanitized = sanitized.replace(/\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/g, '[IP]');
    sanitized = sanitized.replace(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g, '[EMAIL]');
    
    return sanitized;
  }

  /**
   * Get error details for logging (includes context but sanitized)
   */
  public getLoggingDetails(): Record<string, any> {
    return {
      type: this.type,
      severity: this.severity,
      message: this.getSanitizedMessage(),
      recoverable: this.recoverable,
      timestamp: this.timestamp.toISOString(),
      context: this.sanitizeContext(this.context),
      originalErrorType: this.originalError?.name,
      originalErrorMessage: this.originalError?.message
    };
  }

  /**
   * Sanitize context for logging (remove sensitive data)
   */
  private sanitizeContext(context?: Record<string, any>): Record<string, any> | undefined {
    if (!context) return undefined;

    const sanitized: Record<string, any> = {};
    
    for (const [key, value] of Object.entries(context)) {
      // Skip sensitive keys
      if (this.isSensitiveKey(key)) {
        sanitized[key] = '[REDACTED]';
        continue;
      }

      // Sanitize string values
      if (typeof value === 'string') {
        sanitized[key] = this.sanitizeString(value);
      } else if (typeof value === 'object' && value !== null) {
        sanitized[key] = this.sanitizeContext(value as Record<string, any>);
      } else {
        sanitized[key] = value;
      }
    }

    return sanitized;
  }

  /**
   * Check if a key contains sensitive information
   */
  private isSensitiveKey(key: string): boolean {
    const sensitiveKeys = [
      'token', 'password', 'secret', 'key', 'auth', 'credential',
      'email', 'phone', 'address', 'ssn', 'credit', 'payment'
    ];
    
    return sensitiveKeys.some(sensitive => 
      key.toLowerCase().includes(sensitive)
    );
  }

  /**
   * Sanitize string values
   */
  private sanitizeString(value: string): string {
    let sanitized = value;
    
    // Remove URLs, IPs, emails
    sanitized = sanitized.replace(/https?:\/\/[^\s]+/g, '[URL]');
    sanitized = sanitized.replace(/\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/g, '[IP]');
    sanitized = sanitized.replace(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g, '[EMAIL]');
    
    return sanitized;
  }
}

/**
 * User-friendly error messages for different error types
 */
export const ERROR_MESSAGES = {
  AGENT_UNAVAILABLE: "Lenza is currently unavailable. Please try again in a moment.",
  NETWORK_TIMEOUT: "Request timed out. Please try again.",
  PARSING_ERROR: "Received an invalid response. Please try again.",
  RATE_LIMITED: "Too many requests. Please wait a moment before trying again.",
  UNKNOWN_ERROR: "An unexpected error occurred. Please try again.",
  STREAM_INTERRUPTED: "Response was interrupted. Here's what I received so far:",
  APPROVAL_TIMEOUT: "Tool approval request timed out and was automatically denied.",
  VALIDATION_ERROR: "Invalid input provided. Please check your message and try again.",
  PERMISSION_ERROR: "You don't have permission to perform this action.",
  DISCORD_API_ERROR: "Discord API error occurred. Please try again.",
  CONFIGURATION_ERROR: "Bot configuration error. Please contact support.",
  CONNECTION_LOST: "Connection to Lenza was lost. Attempting to reconnect...",
  SERVICE_DEGRADED: "Lenza is experiencing issues. Some features may be limited.",
  MAINTENANCE_MODE: "Lenza is currently under maintenance. Please try again later.",
  CONVERSATION_NOT_FOUND: "No existing conversation found. Use `/lenza-new` to start a new conversation.",
  MESSAGE_TOO_LONG: "Message is too long. Please keep your message under 2000 characters.",
  EMPTY_MESSAGE: "Message cannot be empty. Please provide a message.",
  CHANNEL_RESTRICTED: "This command can only be used in a server channel.",
  RETRY_EXHAUSTED: "Maximum retry attempts reached. Please try again later.",
  PARTIAL_RESPONSE: "Received partial response due to interruption:",
  RECOVERY_SUCCESSFUL: "Connection restored. Continuing...",
  FALLBACK_ACTIVE: "Using fallback service due to primary service issues."
} as const;

/**
 * Get user-friendly error message based on error type and context
 */
export function getUserFriendlyErrorMessage(error: AgentError): string {
  switch (error.type) {
    case 'network':
      return ERROR_MESSAGES.AGENT_UNAVAILABLE;
    case 'timeout':
      return ERROR_MESSAGES.NETWORK_TIMEOUT;
    case 'parsing':
      return ERROR_MESSAGES.PARSING_ERROR;
    case 'rate_limit':
      return ERROR_MESSAGES.RATE_LIMITED;
    case 'validation':
      if (error.message.includes('empty')) {
        return ERROR_MESSAGES.EMPTY_MESSAGE;
      } else if (error.message.includes('long') || error.message.includes('2000')) {
        return ERROR_MESSAGES.MESSAGE_TOO_LONG;
      }
      return ERROR_MESSAGES.VALIDATION_ERROR;
    case 'permission':
      return ERROR_MESSAGES.PERMISSION_ERROR;
    case 'stream_interrupted':
      return ERROR_MESSAGES.STREAM_INTERRUPTED;
    case 'discord_api':
      return ERROR_MESSAGES.DISCORD_API_ERROR;
    case 'configuration':
      return ERROR_MESSAGES.CONFIGURATION_ERROR;
    case 'api':
      if (error.message.includes('rate limit')) {
        return ERROR_MESSAGES.RATE_LIMITED;
      } else if (error.message.includes('404') || error.message.includes('not found')) {
        return ERROR_MESSAGES.CONVERSATION_NOT_FOUND;
      } else if (error.message.includes('maintenance')) {
        return ERROR_MESSAGES.MAINTENANCE_MODE;
      }
      return ERROR_MESSAGES.AGENT_UNAVAILABLE;
    default:
      return ERROR_MESSAGES.UNKNOWN_ERROR;
  }
}

/**
 * Check if an error is retryable based on type and context
 */
export function isRetryableError(error: AgentError): boolean {
  return error.recoverable;
}

/**
 * Get retry delay based on error type and attempt number
 */
export function getRetryDelay(error: AgentError, attempt: number): number {
  const baseDelay = 1000; // 1 second
  
  switch (error.type) {
    case 'rate_limit':
      // Longer delay for rate limits
      return Math.min(baseDelay * Math.pow(2, attempt) * 2, 30000); // Cap at 30 seconds
    case 'network':
    case 'timeout':
      // Standard exponential backoff
      return Math.min(baseDelay * Math.pow(2, attempt), 10000); // Cap at 10 seconds
    case 'stream_interrupted':
      // Quick retry for stream interruptions
      return Math.min(baseDelay * attempt, 5000); // Cap at 5 seconds
    default:
      return baseDelay * Math.pow(2, attempt);
  }
}

/**
 * Determine if error should trigger circuit breaker
 */
export function shouldTriggerCircuitBreaker(error: AgentError): boolean {
  return error.severity === 'critical' || 
         (error.severity === 'high' && !error.recoverable);
}

/**
 * Create error from HTTP response
 */
export function createErrorFromResponse(response: { status: number; statusText: string; url?: string }): AgentError {
  const context = {
    status: response.status,
    statusText: response.statusText,
    url: response.url
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
 * Create error from network failure
 */
export function createNetworkError(originalError: Error, context?: Record<string, any>): AgentError {
  if (originalError.name === 'AbortError' || originalError.message.includes('timeout')) {
    return new AgentError('timeout', 'Request timed out', originalError, context);
  }
  
  if (originalError.message.includes('fetch') || 
      originalError.message.includes('network') ||
      originalError.message.includes('ECONNREFUSED') ||
      originalError.message.includes('ENOTFOUND')) {
    return new AgentError('network', 'Network connection failed', originalError, context);
  }
  
  return new AgentError('unknown', 'Unknown network error', originalError, context);
}