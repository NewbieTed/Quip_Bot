import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { BackendApiClient } from './backend-api-client';
import { AgentError } from '../errors/agent-error';
import { getBotConfig } from '../config/bot-config';

// Mock the config
vi.mock('../config/bot-config', () => ({
  getBotConfig: vi.fn(() => ({
    backendApiUrl: 'http://localhost:8080',
    backendApiTimeout: 30000,
    maxMessageLength: 2000,
    approvalTimeoutSeconds: 60,
    retryAttempts: 3,
    retryDelayMs: 1000
  }))
}));

// Mock fetch
global.fetch = vi.fn();

describe('BackendApiClient', () => {
  let client: BackendApiClient;
  let mockFetch: any;

  beforeEach(() => {
    client = new BackendApiClient();
    mockFetch = vi.mocked(fetch);
    mockFetch.mockClear();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('constructor', () => {
    it('should initialize with correct configuration', () => {
      expect(client).toBeInstanceOf(BackendApiClient);
      expect(getBotConfig).toHaveBeenCalled();
    });
  });

  describe('invokeNewConversation', () => {
    it('should make correct API call for new conversation', async () => {
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({ done: false, value: new TextEncoder().encode('data: {"content":"Hello","type":"update"}\n\n') })
              .mockResolvedValueOnce({ done: true, value: undefined })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Hello Lenza',
        toolWhitelist: ['tool1', 'tool2']
      };

      const generator = client.invokeNewConversation(request);
      const result = await generator.next();

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/assistant/new',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
          },
          body: JSON.stringify(request)
        })
      );

      expect(result.value).toEqual({
        content: 'Hello',
        type: 'update'
      });
    });

    it('should handle API errors correctly', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error'
      });

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Hello Lenza',
        toolWhitelist: []
      };

      const generator = client.invokeNewConversation(request);
      
      await expect(generator.next()).rejects.toThrow(AgentError);
    });

    it('should handle network errors', async () => {
      mockFetch.mockRejectedValue(new Error('Network error'));

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Hello Lenza',
        toolWhitelist: []
      };

      const generator = client.invokeNewConversation(request);
      
      await expect(generator.next()).rejects.toThrow(AgentError);
    });
  });

  describe('invokeConversation', () => {
    it('should make correct API call for existing conversation', async () => {
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({ done: false, value: new TextEncoder().encode('data: {"content":"Response","type":"update"}\n\n') })
              .mockResolvedValueOnce({ done: true, value: undefined })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Continue conversation'
      };

      const generator = client.invokeConversation(request);
      const result = await generator.next();

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/assistant',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
          },
          body: JSON.stringify(request)
        })
      );

      expect(result.value).toEqual({
        content: 'Response',
        type: 'update'
      });
    });
  });

  describe('updateToolWhitelist', () => {
    it('should make correct API call for tool whitelist update', async () => {
      const mockResponse = {
        ok: true,
        json: vi.fn().mockResolvedValue({ success: true })
      };
      mockFetch.mockResolvedValue(mockResponse);

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        addedTools: ['tool1'],
        removedTools: ['tool2']
      };

      const result = await client.updateToolWhitelist(request);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/assistant/tools/whitelist',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(request)
        })
      );

      expect(result).toEqual({ success: true });
    });

    it('should handle whitelist update errors', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 400,
        statusText: 'Bad Request'
      });

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        addedTools: ['tool1'],
        removedTools: []
      };

      await expect(client.updateToolWhitelist(request)).rejects.toThrow(AgentError);
    });
  });

  describe('parseStreamingResponse', () => {
    it('should parse valid SSE data correctly', () => {
      const validData = 'data: {"content":"Hello","type":"update"}\n\n';
      const result = (client as any).parseStreamingResponse(validData);
      
      expect(result).toEqual({
        content: 'Hello',
        type: 'update'
      });
    });

    it('should handle invalid JSON gracefully', () => {
      const invalidData = 'data: invalid json\n\n';
      
      expect(() => {
        (client as any).parseStreamingResponse(invalidData);
      }).toThrow(AgentError);
    });

    it('should handle empty data', () => {
      const emptyData = '';
      const result = (client as any).parseStreamingResponse(emptyData);
      
      expect(result).toBeNull();
    });
  });
});