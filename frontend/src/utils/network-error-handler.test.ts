import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { NetworkErrorHandler } from './network-error-handler';
import { AgentError } from '../errors/agent-error';

describe('NetworkErrorHandler', () => {
  let handler: NetworkErrorHandler;

  beforeEach(() => {
    handler = new NetworkErrorHandler();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  describe('constructor', () => {
    it('should create handler instance', () => {
      expect(handler).toBeInstanceOf(NetworkErrorHandler);
    });
  });

  describe('handleWithRetry', () => {
    it('should return result on successful operation', async () => {
      const operation = vi.fn().mockResolvedValue('success');
      
      const result = await handler.handleWithRetry(operation, 3);
      
      expect(result).toBe('success');
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('should retry on retryable errors', async () => {
      const operation = vi.fn()
        .mockRejectedValueOnce(new AgentError('network', 'Connection failed'))
        .mockRejectedValueOnce(new AgentError('timeout', 'Request timed out'))
        .mockResolvedValue('success');
      
      const resultPromise = handler.handleWithRetry(operation, 3);
      
      // Advance timers to trigger retries
      await vi.advanceTimersByTimeAsync(1000);
      await vi.advanceTimersByTimeAsync(2000);
      
      const result = await resultPromise;
      
      expect(result).toBe('success');
      expect(operation).toHaveBeenCalledTimes(3);
    });

    it('should not retry on non-retryable errors', async () => {
      const operation = vi.fn().mockRejectedValue(new AgentError('parsing', 'Invalid JSON'));
      
      await expect(handler.handleWithRetry(operation, 3)).rejects.toThrow(AgentError);
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('should throw after max retries exceeded', async () => {
      const operation = vi.fn().mockRejectedValue(new AgentError('network', 'Connection failed'));
      
      const resultPromise = handler.handleWithRetry(operation, 2);
      
      // Advance timers for all retry attempts
      await vi.advanceTimersByTimeAsync(1000);
      await vi.advanceTimersByTimeAsync(2000);
      
      await expect(resultPromise).rejects.toThrow(AgentError);
      expect(operation).toHaveBeenCalledTimes(3); // Initial + 2 retries
    });

    it('should use exponential backoff for delays', async () => {
      const operation = vi.fn()
        .mockRejectedValueOnce(new AgentError('network', 'Connection failed'))
        .mockRejectedValueOnce(new AgentError('network', 'Connection failed'))
        .mockResolvedValue('success');
      
      const startTime = Date.now();
      const resultPromise = handler.handleWithRetry(operation, 3);
      
      // First retry after 1000ms
      await vi.advanceTimersByTimeAsync(1000);
      expect(operation).toHaveBeenCalledTimes(2);
      
      // Second retry after 2000ms more
      await vi.advanceTimersByTimeAsync(2000);
      expect(operation).toHaveBeenCalledTimes(3);
      
      const result = await resultPromise;
      expect(result).toBe('success');
    });

    it('should handle generic errors as non-retryable', async () => {
      const operation = vi.fn().mockRejectedValue(new Error('Generic error'));
      
      await expect(handler.handleWithRetry(operation, 3)).rejects.toThrow(Error);
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('should handle zero max retries', async () => {
      const operation = vi.fn().mockRejectedValue(new AgentError('network', 'Connection failed'));
      
      await expect(handler.handleWithRetry(operation, 0)).rejects.toThrow(AgentError);
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('should handle negative max retries', async () => {
      const operation = vi.fn().mockRejectedValue(new AgentError('network', 'Connection failed'));
      
      await expect(handler.handleWithRetry(operation, -1)).rejects.toThrow(AgentError);
      expect(operation).toHaveBeenCalledTimes(1);
    });
  });

  describe('calculateBackoffDelay', () => {
    it('should calculate correct exponential backoff delays', () => {
      const delay1 = handler.calculateBackoffDelay(1);
      const delay2 = handler.calculateBackoffDelay(2);
      const delay3 = handler.calculateBackoffDelay(3);
      
      expect(delay1).toBe(1000); // 1000 * 2^0
      expect(delay2).toBe(2000); // 1000 * 2^1
      expect(delay3).toBe(4000); // 1000 * 2^2
    });

    it('should handle zero attempt', () => {
      const delay = handler.calculateBackoffDelay(0);
      expect(delay).toBe(500); // 1000 * 2^-1
    });

    it('should cap maximum delay', () => {
      const delay = handler.calculateBackoffDelay(10);
      expect(delay).toBeLessThanOrEqual(30000); // Should have reasonable maximum
    });
  });

  describe('isRetryableError', () => {
    it('should identify retryable AgentError types', () => {
      const networkError = new AgentError('network', 'Connection failed');
      const timeoutError = new AgentError('timeout', 'Request timed out');
      const apiError = new AgentError('api', 'Server error');
      
      expect(handler.isRetryableError(networkError)).toBe(true);
      expect(handler.isRetryableError(timeoutError)).toBe(true);
      expect(handler.isRetryableError(apiError)).toBe(true);
    });

    it('should identify non-retryable AgentError types', () => {
      const parsingError = new AgentError('parsing', 'Invalid JSON');
      const unknownError = new AgentError('unknown', 'Unknown error');
      
      expect(handler.isRetryableError(parsingError)).toBe(false);
      expect(handler.isRetryableError(unknownError)).toBe(false);
    });

    it('should handle generic errors as non-retryable', () => {
      const genericError = new Error('Generic error');
      const typeError = new TypeError('Type error');
      
      expect(handler.isRetryableError(genericError)).toBe(false);
      expect(handler.isRetryableError(typeError)).toBe(false);
    });

    it('should handle non-error objects', () => {
      expect(handler.isRetryableError('string error' as any)).toBe(false);
      expect(handler.isRetryableError(null as any)).toBe(false);
      expect(handler.isRetryableError(undefined as any)).toBe(false);
      expect(handler.isRetryableError({} as any)).toBe(false);
    });
  });

  describe('error handling edge cases', () => {
    it('should handle operation that throws non-Error objects', async () => {
      const operation = vi.fn().mockRejectedValue('string error');
      
      await expect(handler.handleWithRetry(operation, 3)).rejects.toBe('string error');
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('should handle operation that throws null', async () => {
      const operation = vi.fn().mockRejectedValue(null);
      
      await expect(handler.handleWithRetry(operation, 3)).rejects.toBe(null);
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('should handle operation that throws undefined', async () => {
      const operation = vi.fn().mockRejectedValue(undefined);
      
      await expect(handler.handleWithRetry(operation, 3)).rejects.toBe(undefined);
      expect(operation).toHaveBeenCalledTimes(1);
    });
  });

  describe('concurrent operations', () => {
    it('should handle multiple concurrent operations', async () => {
      const operation1 = vi.fn().mockResolvedValue('result1');
      const operation2 = vi.fn().mockResolvedValue('result2');
      
      const [result1, result2] = await Promise.all([
        handler.handleWithRetry(operation1, 3),
        handler.handleWithRetry(operation2, 3)
      ]);
      
      expect(result1).toBe('result1');
      expect(result2).toBe('result2');
      expect(operation1).toHaveBeenCalledTimes(1);
      expect(operation2).toHaveBeenCalledTimes(1);
    });

    it('should handle concurrent operations with different retry patterns', async () => {
      const operation1 = vi.fn()
        .mockRejectedValueOnce(new AgentError('network', 'Failed'))
        .mockResolvedValue('success1');
      const operation2 = vi.fn().mockResolvedValue('success2');
      
      const promise1 = handler.handleWithRetry(operation1, 3);
      const promise2 = handler.handleWithRetry(operation2, 3);
      
      // Advance timer for operation1 retry
      await vi.advanceTimersByTimeAsync(1000);
      
      const [result1, result2] = await Promise.all([promise1, promise2]);
      
      expect(result1).toBe('success1');
      expect(result2).toBe('success2');
      expect(operation1).toHaveBeenCalledTimes(2);
      expect(operation2).toHaveBeenCalledTimes(1);
    });
  });
});