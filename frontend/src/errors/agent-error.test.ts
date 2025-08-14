import { describe, it, expect } from 'vitest';
import { AgentError, getUserFriendlyErrorMessage } from './agent-error';

describe('AgentError', () => {
  describe('constructor', () => {
    it('should create error with correct properties', () => {
      const error = new AgentError('network', 'Connection failed');
      
      expect(error).toBeInstanceOf(Error);
      expect(error).toBeInstanceOf(AgentError);
      expect(error.type).toBe('network');
      expect(error.message).toBe('Connection failed');
      expect(error.name).toBe('AgentError');
    });

    it('should handle all error types', () => {
      const networkError = new AgentError('network', 'Network error');
      const timeoutError = new AgentError('timeout', 'Timeout error');
      const parsingError = new AgentError('parsing', 'Parsing error');
      const apiError = new AgentError('api', 'API error');
      const unknownError = new AgentError('unknown', 'Unknown error');

      expect(networkError.type).toBe('network');
      expect(timeoutError.type).toBe('timeout');
      expect(parsingError.type).toBe('parsing');
      expect(apiError.type).toBe('api');
      expect(unknownError.type).toBe('unknown');
    });

    it('should include original error when provided', () => {
      const originalError = new Error('Original error');
      const agentError = new AgentError('network', 'Network failed', originalError);
      
      expect(agentError.originalError).toBe(originalError);
    });

    it('should include context when provided', () => {
      const context = { userId: '123', channelId: '456' };
      const agentError = new AgentError('api', 'API failed', undefined, context);
      
      expect(agentError.context).toEqual(context);
    });
  });

  describe('stack trace', () => {
    it('should have proper stack trace', () => {
      const error = new AgentError('network', 'Connection failed');
      
      expect(error.stack).toBeDefined();
      expect(error.stack).toContain('AgentError');
    });
  });
});

describe('getUserFriendlyErrorMessage', () => {
  describe('AgentError handling', () => {
    it('should return network error message', () => {
      const error = new AgentError('network', 'Connection failed');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('Lenza is currently unavailable. Please try again in a moment.');
    });

    it('should return timeout error message', () => {
      const error = new AgentError('timeout', 'Request timed out');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('Request timed out. Please try again.');
    });

    it('should return parsing error message', () => {
      const error = new AgentError('parsing', 'Invalid JSON');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('Received an invalid response. Please try again.');
    });

    it('should return API error message', () => {
      const error = new AgentError('api', 'API returned 500');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('Service temporarily unavailable. Please try again.');
    });

    it('should return rate limit error message', () => {
      const error = new AgentError('rate_limit', 'Too many requests');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('Too many requests. Please wait a moment before trying again.');
    });

    it('should return unknown error message', () => {
      const error = new AgentError('unknown', 'Something went wrong');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });
  });

  describe('generic Error handling', () => {
    it('should handle generic Error objects', () => {
      const error = new Error('Generic error');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });

    it('should handle errors with specific messages', () => {
      const error = new Error('Network request failed');
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });
  });

  describe('string error handling', () => {
    it('should handle string errors', () => {
      const message = getUserFriendlyErrorMessage('Something went wrong');
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });

    it('should handle empty string errors', () => {
      const message = getUserFriendlyErrorMessage('');
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });
  });

  describe('null/undefined handling', () => {
    it('should handle null errors', () => {
      const message = getUserFriendlyErrorMessage(null);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });

    it('should handle undefined errors', () => {
      const message = getUserFriendlyErrorMessage(undefined);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });
  });

  describe('object error handling', () => {
    it('should handle plain objects', () => {
      const error = { message: 'Object error' };
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });

    it('should handle objects with error properties', () => {
      const error = { error: 'Something failed', code: 500 };
      const message = getUserFriendlyErrorMessage(error);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });
  });

  describe('special error cases', () => {
    it('should handle Discord API errors', () => {
      const discordError = new Error('Missing Permissions');
      discordError.name = 'DiscordAPIError';
      
      const message = getUserFriendlyErrorMessage(discordError);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });

    it('should handle fetch errors', () => {
      const fetchError = new Error('fetch failed');
      fetchError.name = 'TypeError';
      
      const message = getUserFriendlyErrorMessage(fetchError);
      
      expect(message).toBe('An unexpected error occurred. Please try again.');
    });
  });

  describe('error message consistency', () => {
    it('should always return a string', () => {
      const testCases = [
        new AgentError('network', 'test'),
        new Error('test'),
        'string error',
        null,
        undefined,
        123,
        {},
        []
      ];

      testCases.forEach(testCase => {
        const message = getUserFriendlyErrorMessage(testCase);
        expect(typeof message).toBe('string');
        expect(message.length).toBeGreaterThan(0);
      });
    });

    it('should not expose internal error details', () => {
      const sensitiveError = new AgentError('api', 'Database connection string: postgres://user:pass@host/db');
      const message = getUserFriendlyErrorMessage(sensitiveError);
      
      expect(message).not.toContain('postgres://');
      expect(message).not.toContain('user:pass');
      expect(message).toBe('Service temporarily unavailable. Please try again.');
    });
  });
});