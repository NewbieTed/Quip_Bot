import { AgentError, isRetryableError, getRetryDelay, shouldTriggerCircuitBreaker, createNetworkError } from '../errors/agent-error';
import { getBotConfig } from '../config/bot-config';
import { logger } from './logger';

/**
 * Circuit breaker states
 */
type CircuitState = 'closed' | 'open' | 'half-open';

/**
 * Circuit breaker configuration
 */
interface CircuitBreakerConfig {
  failureThreshold: number;
  recoveryTimeout: number;
  monitoringPeriod: number;
}

/**
 * Operation context for enhanced error handling
 */
interface OperationContext {
  operationName?: string;
  userId?: string;
  channelId?: string;
  timeout?: number;
  priority?: 'low' | 'normal' | 'high';
  [key: string]: any;
}

/**
 * Enhanced network error handler with circuit breaker pattern and comprehensive retry logic
 */
export class NetworkErrorHandler {
  private config = getBotConfig();
  private circuitState: CircuitState = 'closed';
  private failureCount: number = 0;
  private lastFailureTime: number = 0;
  private circuitConfig: CircuitBreakerConfig = {
    failureThreshold: 5,
    recoveryTimeout: 30000, // 30 seconds
    monitoringPeriod: 60000  // 1 minute
  };
  private operationMetrics: Map<string, { attempts: number; failures: number; lastAttempt: number }> = new Map();

  /**
   * Execute an operation with comprehensive error handling and retry logic
   */
  async handleWithRetry<T>(
    operation: () => Promise<T>,
    maxRetries: number = this.config.retryAttempts,
    context?: OperationContext
  ): Promise<T> {
    const operationName = context?.operationName || 'unknown_operation';
    const startTime = Date.now();
    
    // Check circuit breaker
    if (this.circuitState === 'open') {
      if (Date.now() - this.lastFailureTime < this.circuitConfig.recoveryTimeout) {
        throw new AgentError('api', 'Service temporarily unavailable due to circuit breaker', undefined, {
          circuitState: this.circuitState,
          ...context
        });
      } else {
        // Try to transition to half-open
        this.circuitState = 'half-open';
        logger.info('Circuit breaker transitioning to half-open state');
      }
    }

    let lastError: AgentError | undefined;
    const operationId = this.generateOperationId();

    // Update operation metrics
    this.updateOperationMetrics(operationName, 'attempt');

    logger.debug(`Starting operation ${operationName} (${operationId}) with max retries: ${maxRetries}`, context);

    for (let attempt = 1; attempt <= maxRetries + 1; attempt++) {
      try {
        const result = await this.executeWithTimeout(operation, context?.timeout);
        
        // Success - reset circuit breaker if needed
        if (this.circuitState === 'half-open') {
          this.circuitState = 'closed';
          this.failureCount = 0;
          logger.info('Circuit breaker reset to closed state after successful operation');
        }

        const duration = Date.now() - startTime;
        logger.performance(operationName, duration, { 
          operationId, 
          attempt, 
          success: true,
          ...context 
        });

        return result;

      } catch (error) {
        const agentError = this.convertToAgentError(error, { 
          operationId, 
          attempt, 
          operationName,
          ...context 
        });
        lastError = agentError;

        // Update failure metrics
        this.updateOperationMetrics(operationName, 'failure');

        logger.warn(`Operation ${operationName} (${operationId}) failed on attempt ${attempt}/${maxRetries + 1}`, {
          errorType: agentError.type,
          errorMessage: agentError.getSanitizedMessage(),
          severity: agentError.severity,
          recoverable: agentError.recoverable,
          ...context
        });

        // Check if we should trigger circuit breaker
        if (shouldTriggerCircuitBreaker(agentError)) {
          this.handleCircuitBreakerTrigger(agentError);
        }

        // Don't retry on the last attempt or if error is not retryable
        if (attempt > maxRetries || !isRetryableError(agentError)) {
          const duration = Date.now() - startTime;
          logger.error(`Operation ${operationName} (${operationId}) failed permanently after ${attempt} attempts`, agentError, {
            duration,
            finalAttempt: attempt,
            ...context
          });
          throw agentError;
        }

        // Calculate delay and wait before retrying
        const delay = this.calculateRetryDelay(agentError, attempt, context);
        logger.debug(`Retrying operation ${operationName} (${operationId}) in ${delay}ms`, {
          attempt,
          nextAttempt: attempt + 1,
          delay
        });
        
        await this.sleep(delay);
      }
    }

    // This should never be reached, but TypeScript requires it
    throw lastError || new AgentError('unknown', 'Unexpected error in retry logic');
  }

  /**
   * Execute operation with timeout
   */
  private async executeWithTimeout<T>(
    operation: () => Promise<T>,
    timeoutMs?: number
  ): Promise<T> {
    if (!timeoutMs) {
      return await operation();
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      // Note: This assumes the operation supports AbortController
      // In practice, you'd need to modify operations to accept the signal
      return await operation();
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        throw new AgentError('timeout', `Operation timed out after ${timeoutMs}ms`);
      }
      throw error;
    } finally {
      clearTimeout(timeoutId);
    }
  }

  /**
   * Calculate retry delay with enhanced logic
   */
  private calculateRetryDelay(error: AgentError, attempt: number, context?: OperationContext): number {
    const baseDelay = getRetryDelay(error, attempt);
    
    // Adjust delay based on priority
    if (context?.priority === 'high') {
      return Math.max(baseDelay * 0.5, 500); // Faster retry for high priority
    } else if (context?.priority === 'low') {
      return baseDelay * 1.5; // Slower retry for low priority
    }
    
    return baseDelay;
  }

  /**
   * Handle circuit breaker trigger
   */
  private handleCircuitBreakerTrigger(error: AgentError): void {
    this.failureCount++;
    this.lastFailureTime = Date.now();

    if (this.failureCount >= this.circuitConfig.failureThreshold) {
      this.circuitState = 'open';
      logger.critical('Circuit breaker opened due to repeated failures', error, {
        failureCount: this.failureCount,
        threshold: this.circuitConfig.failureThreshold
      });
    }
  }

  /**
   * Update operation metrics
   */
  private updateOperationMetrics(operationName: string, type: 'attempt' | 'failure'): void {
    const metrics = this.operationMetrics.get(operationName) || {
      attempts: 0,
      failures: 0,
      lastAttempt: 0
    };

    if (type === 'attempt') {
      metrics.attempts++;
      metrics.lastAttempt = Date.now();
    } else if (type === 'failure') {
      metrics.failures++;
    }

    this.operationMetrics.set(operationName, metrics);

    // Clean up old metrics
    this.cleanupOldMetrics();
  }

  /**
   * Clean up old operation metrics
   */
  private cleanupOldMetrics(): void {
    const now = Date.now();
    const maxAge = this.circuitConfig.monitoringPeriod;

    this.operationMetrics.forEach((metrics, operationName) => {
      if (now - metrics.lastAttempt > maxAge) {
        this.operationMetrics.delete(operationName);
      }
    });
  }

  /**
   * Get operation health metrics
   */
  public getOperationMetrics(): Record<string, { attempts: number; failures: number; successRate: number }> {
    const result: Record<string, { attempts: number; failures: number; successRate: number }> = {};

    this.operationMetrics.forEach((metrics, operationName) => {
      const successRate = metrics.attempts > 0 
        ? ((metrics.attempts - metrics.failures) / metrics.attempts) * 100 
        : 100;

      result[operationName] = {
        attempts: metrics.attempts,
        failures: metrics.failures,
        successRate: Math.round(successRate * 100) / 100
      };
    });

    return result;
  }

  /**
   * Get circuit breaker status
   */
  public getCircuitBreakerStatus(): { state: CircuitState; failureCount: number; lastFailureTime: number } {
    return {
      state: this.circuitState,
      failureCount: this.failureCount,
      lastFailureTime: this.lastFailureTime
    };
  }

  /**
   * Manually reset circuit breaker (for admin operations)
   */
  public resetCircuitBreaker(): void {
    this.circuitState = 'closed';
    this.failureCount = 0;
    this.lastFailureTime = 0;
    logger.info('Circuit breaker manually reset');
  }

  /**
   * Convert various error types to AgentError with enhanced detection
   */
  private convertToAgentError(error: unknown, context?: Record<string, any>): AgentError {
    if (error instanceof AgentError) {
      return error;
    }

    if (error instanceof Error) {
      return createNetworkError(error, context);
    }

    // Handle non-Error objects
    if (typeof error === 'object' && error !== null) {
      const errorObj = error as any;
      if (errorObj.code || errorObj.status) {
        return new AgentError('api', `API error: ${errorObj.message || 'Unknown error'}`, undefined, {
          code: errorObj.code,
          status: errorObj.status,
          ...context
        });
      }
    }

    // Unknown error type
    return new AgentError('unknown', 'An unknown error occurred', undefined, context);
  }

  /**
   * Generate unique operation ID for tracking
   */
  private generateOperationId(): string {
    return `op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Sleep for specified milliseconds
   */
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Check if service is healthy based on recent metrics
   */
  public isServiceHealthy(): boolean {
    if (this.circuitState === 'open') {
      return false;
    }

    // Check recent failure rates
    const recentMetrics = this.getOperationMetrics();
    const overallFailureRate = Object.values(recentMetrics)
      .reduce((acc, metrics) => {
        const failureRate = (metrics.failures / metrics.attempts) * 100;
        return acc + failureRate;
      }, 0) / Object.keys(recentMetrics).length;

    return overallFailureRate < 50; // Consider unhealthy if >50% failure rate
  }
}