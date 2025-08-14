import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { BackendApiClient } from '../../services/backend-api-client';
import { ConversationManager } from '../../services/conversation-manager';
import { StreamingResponseHandler } from '../../services/streaming-response-handler';
import { ToolApprovalHandler } from '../../services/tool-approval-handler';

// Mock fetch for API calls
global.fetch = vi.fn();

// Mock Discord.js components
vi.mock('discord.js', () => ({
  SlashCommandBuilder: class MockSlashCommandBuilder {
    setName = vi.fn().mockReturnThis();
    setDescription = vi.fn().mockReturnThis();
    addStringOption = vi.fn().mockReturnThis();
    toJSON = vi.fn().mockReturnValue({
      name: 'test-command',
      description: 'Test command',
      options: []
    });
  },
  ActionRowBuilder: vi.fn(() => ({
    addComponents: vi.fn().mockReturnThis()
  })),
  ButtonBuilder: vi.fn(() => ({
    setCustomId: vi.fn().mockReturnThis(),
    setLabel: vi.fn().mockReturnThis(),
    setStyle: vi.fn().mockReturnThis(),
    setEmoji: vi.fn().mockReturnThis()
  })),
  ButtonStyle: {
    Success: 3,
    Danger: 4,
    Primary: 1
  }
}));

// Mock config
vi.mock('../../config/bot-config', () => ({
  getBotConfig: vi.fn(() => ({
    backendApiUrl: 'http://localhost:8080',
    backendApiTimeout: 30000,
    maxMessageLength: 2000,
    approvalTimeoutSeconds: 60,
    retryAttempts: 3,
    retryDelayMs: 1000
  }))
}));

describe('Command Integration Tests', () => {
  let mockFetch: any;
  let mockInteraction: any;

  beforeEach(() => {
    mockFetch = vi.mocked(fetch);
    mockFetch.mockClear();

    mockInteraction = {
      user: { id: 'user123', username: 'testuser' },
      channelId: 'channel123',
      guildId: 'guild123',
      channel: { name: 'test-channel' },
      guild: { name: 'test-guild' },
      options: {
        getString: vi.fn()
      },
      deferReply: vi.fn().mockResolvedValue({}),
      editReply: vi.fn().mockResolvedValue({
        createMessageComponentCollector: vi.fn().mockReturnValue({
          on: vi.fn(),
          stop: vi.fn()
        })
      }),
      followUp: vi.fn().mockResolvedValue({})
    };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('End-to-End New Conversation Flow', () => {
    it('should complete full new conversation workflow', async () => {
      // Mock successful API response
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"Hello! How can I help you?","type":"update"}\n\n')
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      // Set up interaction
      mockInteraction.options.getString.mockReturnValue('Hello Lenza!');

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();
      const streamingHandler = new StreamingResponseHandler(mockInteraction);

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      conversationManager.validateMessage('Hello Lenza!');
      const payload = conversationManager.buildNewConversationPayload(context, 'Hello Lenza!');
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      const responseStream = apiClient.invokeNewConversation(backendRequest);
      await streamingHandler.handleStream(responseStream);

      // Verify API call
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/assistant/new',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
          },
          body: JSON.stringify({
            serverId: 'guild123',
            channelId: 'channel123',
            memberId: 'user123',
            message: 'Hello Lenza!',
            toolWhitelist: []
          })
        })
      );

      // Verify Discord interaction
      expect(mockInteraction.editReply).toHaveBeenCalledWith('Hello! How can I help you?');
    });

    it('should handle tool approval in new conversation', async () => {
      // Mock API response with tool approval request
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"I need to use a tool","type":"interrupt","tool_name":"test_tool"}\n\n')
              })
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"Tool approved, continuing...","type":"update"}\n\n')
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      mockInteraction.options.getString.mockReturnValue('Use a tool please');

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();
      const streamingHandler = new StreamingResponseHandler(mockInteraction);

      // Mock tool approval
      const mockCollector = {
        on: vi.fn(),
        stop: vi.fn()
      };
      mockInteraction.editReply.mockResolvedValue({
        createMessageComponentCollector: vi.fn().mockReturnValue(mockCollector)
      });

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      const payload = conversationManager.buildNewConversationPayload(context, 'Use a tool please');
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      const responseStream = apiClient.invokeNewConversation(backendRequest);
      const streamPromise = streamingHandler.handleStream(responseStream);

      // Simulate tool approval
      const collectCallback = mockCollector.on.mock.calls.find(call => call[0] === 'collect')[1];
      const mockButtonInteraction = {
        user: { id: 'user123' },
        customId: 'tool_approval_approve_test123',
        update: vi.fn().mockResolvedValue({})
      };
      collectCallback(mockButtonInteraction);

      await streamPromise;

      // Verify tool approval UI was shown
      expect(mockInteraction.editReply).toHaveBeenCalledWith(
        expect.objectContaining({
          content: expect.stringContaining('test_tool'),
          components: expect.any(Array)
        })
      );

      // Verify final message was updated
      expect(mockInteraction.editReply).toHaveBeenCalledWith('Tool approved, continuing...');
    });
  });

  describe('End-to-End Resume Conversation Flow', () => {
    it('should complete full resume conversation workflow', async () => {
      // Mock successful API response
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"Continuing our conversation...","type":"update"}\n\n')
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      mockInteraction.options.getString.mockReturnValue('Continue please');

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();
      const streamingHandler = new StreamingResponseHandler(mockInteraction);

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      conversationManager.validateMessage('Continue please');
      const payload = conversationManager.buildResumeConversationPayload(context, 'Continue please');
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      const responseStream = apiClient.invokeConversation(backendRequest);
      await streamingHandler.handleStream(responseStream);

      // Verify API call
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/assistant',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
          },
          body: JSON.stringify({
            serverId: 'guild123',
            channelId: 'channel123',
            memberId: 'user123',
            message: 'Continue please'
          })
        })
      );

      // Verify Discord interaction
      expect(mockInteraction.editReply).toHaveBeenCalledWith('Continuing our conversation...');
    });

    it('should handle resume without message', async () => {
      // Mock successful API response
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"What would you like to know?","type":"update"}\n\n')
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      mockInteraction.options.getString.mockReturnValue(null);

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();
      const streamingHandler = new StreamingResponseHandler(mockInteraction);

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      const payload = conversationManager.buildResumeConversationPayload(context);
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      const responseStream = apiClient.invokeConversation(backendRequest);
      await streamingHandler.handleStream(responseStream);

      // Verify API call without message
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/assistant',
        expect.objectContaining({
          body: JSON.stringify({
            serverId: 'guild123',
            channelId: 'channel123',
            memberId: 'user123'
          })
        })
      );
    });
  });

  describe('Error Handling Integration', () => {
    it('should handle API errors gracefully', async () => {
      // Mock API error
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error'
      });

      mockInteraction.options.getString.mockReturnValue('Hello Lenza!');

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      const payload = conversationManager.buildNewConversationPayload(context, 'Hello Lenza!');
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      // Should throw AgentError
      await expect(async () => {
        const responseStream = apiClient.invokeNewConversation(backendRequest);
        await responseStream.next();
      }).rejects.toThrow();
    });

    it('should handle network errors gracefully', async () => {
      // Mock network error
      mockFetch.mockRejectedValue(new Error('Network error'));

      mockInteraction.options.getString.mockReturnValue('Hello Lenza!');

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      const payload = conversationManager.buildNewConversationPayload(context, 'Hello Lenza!');
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      // Should throw AgentError
      await expect(async () => {
        const responseStream = apiClient.invokeNewConversation(backendRequest);
        await responseStream.next();
      }).rejects.toThrow();
    });
  });

  describe('Message Splitting Integration', () => {
    it('should handle long messages correctly', async () => {
      // Mock API response with long content
      const longContent = 'a'.repeat(3000);
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode(`data: {"content":"${longContent}","type":"update"}\n\n`)
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };
      mockFetch.mockResolvedValue(mockResponse);

      mockInteraction.options.getString.mockReturnValue('Tell me a long story');

      // Create services
      const apiClient = new BackendApiClient();
      const conversationManager = new ConversationManager();
      const streamingHandler = new StreamingResponseHandler(mockInteraction);

      // Execute workflow
      const context = conversationManager.extractContextFromInteraction(mockInteraction);
      const payload = conversationManager.buildNewConversationPayload(context, 'Tell me a long story');
      const backendRequest = conversationManager.convertToBackendRequest(payload);
      
      const responseStream = apiClient.invokeNewConversation(backendRequest);
      await streamingHandler.handleStream(responseStream);

      // Verify message was split
      expect(mockInteraction.editReply).toHaveBeenCalled();
      expect(mockInteraction.followUp).toHaveBeenCalled();
    });
  });

  describe('Concurrent Operations', () => {
    it('should handle multiple concurrent conversations', async () => {
      // Mock successful API responses
      const mockResponse1 = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"Response 1","type":"update"}\n\n')
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };

      const mockResponse2 = {
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn()
              .mockResolvedValueOnce({
                done: false,
                value: new TextEncoder().encode('data: {"content":"Response 2","type":"update"}\n\n')
              })
              .mockResolvedValueOnce({
                done: true,
                value: undefined
              })
          })
        }
      };

      mockFetch
        .mockResolvedValueOnce(mockResponse1)
        .mockResolvedValueOnce(mockResponse2);

      // Create two interactions
      const interaction1 = { ...mockInteraction, user: { id: 'user1', username: 'user1' } };
      const interaction2 = { ...mockInteraction, user: { id: 'user2', username: 'user2' } };

      interaction1.options.getString = vi.fn().mockReturnValue('Message 1');
      interaction2.options.getString = vi.fn().mockReturnValue('Message 2');

      // Create services for both
      const apiClient1 = new BackendApiClient();
      const apiClient2 = new BackendApiClient();
      const conversationManager = new ConversationManager();
      const streamingHandler1 = new StreamingResponseHandler(interaction1);
      const streamingHandler2 = new StreamingResponseHandler(interaction2);

      // Execute both workflows concurrently
      const context1 = conversationManager.extractContextFromInteraction(interaction1);
      const context2 = conversationManager.extractContextFromInteraction(interaction2);
      
      const payload1 = conversationManager.buildNewConversationPayload(context1, 'Message 1');
      const payload2 = conversationManager.buildNewConversationPayload(context2, 'Message 2');
      
      const request1 = conversationManager.convertToBackendRequest(payload1);
      const request2 = conversationManager.convertToBackendRequest(payload2);

      const stream1 = apiClient1.invokeNewConversation(request1);
      const stream2 = apiClient2.invokeNewConversation(request2);

      await Promise.all([
        streamingHandler1.handleStream(stream1),
        streamingHandler2.handleStream(stream2)
      ]);

      // Verify both API calls were made
      expect(mockFetch).toHaveBeenCalledTimes(2);
      
      // Verify both interactions were updated
      expect(interaction1.editReply).toHaveBeenCalledWith('Response 1');
      expect(interaction2.editReply).toHaveBeenCalledWith('Response 2');
    });
  });
});