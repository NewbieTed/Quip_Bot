import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SlashCommandBuilder } from 'discord.js';

// Mock all dependencies
vi.mock('../../services/backend-api-client');
vi.mock('../../services/conversation-manager');
vi.mock('../../services/streaming-response-handler');
vi.mock('../../errors/agent-error');

const mockBackendApiClient = {
  invokeConversation: vi.fn()
};

const mockConversationManager = {
  extractContextFromInteraction: vi.fn(),
  buildResumeConversationPayload: vi.fn(),
  convertToBackendRequest: vi.fn(),
  validateMessage: vi.fn()
};

const mockStreamingResponseHandler = {
  handleStream: vi.fn()
};

const mockGetUserFriendlyErrorMessage = vi.fn();

vi.mock('../../services/backend-api-client', () => ({
  BackendApiClient: vi.fn(() => mockBackendApiClient)
}));

vi.mock('../../services/conversation-manager', () => ({
  ConversationManager: vi.fn(() => mockConversationManager)
}));

vi.mock('../../services/streaming-response-handler', () => ({
  StreamingResponseHandler: vi.fn(() => mockStreamingResponseHandler)
}));

vi.mock('../../errors/agent-error', () => ({
  getUserFriendlyErrorMessage: mockGetUserFriendlyErrorMessage,
  AgentError: class AgentError extends Error {
    constructor(public type: string, message: string) {
      super(message);
    }
  }
}));

describe('lenza-resume command', () => {
  let command: any;
  let mockInteraction: any;

  beforeEach(async () => {
    // Reset all mocks
    vi.clearAllMocks();
    
    // Import the command after mocks are set up
    command = await import('./lenza-resume');
    
    mockInteraction = {
      user: { id: 'user123', username: 'testuser' },
      channelId: 'channel123',
      guildId: 'guild123',
      options: {
        getString: vi.fn().mockReturnValue('Continue conversation')
      },
      deferReply: vi.fn().mockResolvedValue({}),
      editReply: vi.fn().mockResolvedValue({})
    };

    // Set up default mock returns
    mockConversationManager.extractContextFromInteraction.mockReturnValue({
      serverId: 'guild123',
      channelId: 'channel123',
      memberId: 'user123',
      username: 'testuser'
    });

    mockConversationManager.buildResumeConversationPayload.mockReturnValue({
      serverId: 'guild123',
      channelId: 'channel123',
      memberId: 'user123',
      message: 'Continue conversation'
    });

    mockConversationManager.convertToBackendRequest.mockReturnValue({
      serverId: 'guild123',
      channelId: 'channel123',
      memberId: 'user123',
      message: 'Continue conversation'
    });

    mockBackendApiClient.invokeConversation.mockReturnValue(async function* () {
      yield { content: 'Continuing our conversation...', type: 'update' };
    }());
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('command data', () => {
    it('should have correct command structure', () => {
      expect(command.data).toBeInstanceOf(SlashCommandBuilder);
      
      const commandData = command.data.toJSON();
      expect(commandData.name).toBe('lenza-resume');
      expect(commandData.description).toBe('Continue an existing conversation with Lenza');
      
      // Should have an optional message option
      const messageOption = commandData.options?.find((opt: any) => opt.name === 'message');
      expect(messageOption).toBeDefined();
      expect(messageOption.required).toBe(false);
      expect(messageOption.type).toBe(3); // STRING type
    });
  });

  describe('execute function', () => {
    it('should execute successfully with message', async () => {
      await command.execute(mockInteraction);

      expect(mockInteraction.deferReply).toHaveBeenCalled();
      expect(mockConversationManager.extractContextFromInteraction).toHaveBeenCalledWith(mockInteraction);
      expect(mockConversationManager.validateMessage).toHaveBeenCalledWith('Continue conversation');
      expect(mockBackendApiClient.invokeConversation).toHaveBeenCalled();
      expect(mockStreamingResponseHandler.handleStream).toHaveBeenCalled();
    });

    it('should execute successfully without message', async () => {
      mockInteraction.options.getString.mockReturnValue(null);

      await command.execute(mockInteraction);

      expect(mockInteraction.deferReply).toHaveBeenCalled();
      expect(mockConversationManager.extractContextFromInteraction).toHaveBeenCalledWith(mockInteraction);
      expect(mockConversationManager.validateMessage).not.toHaveBeenCalled();
      expect(mockBackendApiClient.invokeConversation).toHaveBeenCalled();
      expect(mockStreamingResponseHandler.handleStream).toHaveBeenCalled();
    });

    it('should handle validation errors', async () => {
      const validationError = new Error('Message too long');
      mockConversationManager.validateMessage.mockImplementation(() => {
        throw validationError;
      });
      mockGetUserFriendlyErrorMessage.mockReturnValue('Message is too long');

      await command.execute(mockInteraction);

      expect(mockInteraction.editReply).toHaveBeenCalledWith('Message is too long');
      expect(mockBackendApiClient.invokeConversation).not.toHaveBeenCalled();
    });

    it('should handle API errors', async () => {
      const apiError = new Error('API unavailable');
      mockBackendApiClient.invokeConversation.mockImplementation(() => {
        throw apiError;
      });
      mockGetUserFriendlyErrorMessage.mockReturnValue('Service temporarily unavailable');

      await command.execute(mockInteraction);

      expect(mockInteraction.editReply).toHaveBeenCalledWith('Service temporarily unavailable');
    });

    it('should handle streaming errors', async () => {
      const streamError = new Error('Stream interrupted');
      mockStreamingResponseHandler.handleStream.mockImplementation(() => {
        throw streamError;
      });
      mockGetUserFriendlyErrorMessage.mockReturnValue('Connection interrupted');

      await command.execute(mockInteraction);

      expect(mockInteraction.editReply).toHaveBeenCalledWith('Connection interrupted');
    });

    it('should pass correct parameters to conversation manager with message', async () => {
      await command.execute(mockInteraction);

      expect(mockConversationManager.buildResumeConversationPayload).toHaveBeenCalledWith(
        expect.objectContaining({
          serverId: 'guild123',
          channelId: 'channel123',
          memberId: 'user123',
          username: 'testuser'
        }),
        'Continue conversation'
      );
    });

    it('should pass correct parameters to conversation manager without message', async () => {
      mockInteraction.options.getString.mockReturnValue(null);

      await command.execute(mockInteraction);

      expect(mockConversationManager.buildResumeConversationPayload).toHaveBeenCalledWith(
        expect.objectContaining({
          serverId: 'guild123',
          channelId: 'channel123',
          memberId: 'user123',
          username: 'testuser'
        }),
        undefined
      );
    });

    it('should create streaming response handler with correct interaction', async () => {
      const { StreamingResponseHandler } = await import('../../services/streaming-response-handler');
      
      await command.execute(mockInteraction);

      expect(StreamingResponseHandler).toHaveBeenCalledWith(mockInteraction);
    });

    it('should handle empty string message as no message', async () => {
      mockInteraction.options.getString.mockReturnValue('');

      await command.execute(mockInteraction);

      expect(mockConversationManager.validateMessage).not.toHaveBeenCalled();
      expect(mockConversationManager.buildResumeConversationPayload).toHaveBeenCalledWith(
        expect.any(Object),
        undefined
      );
    });

    it('should handle whitespace-only message as no message', async () => {
      mockInteraction.options.getString.mockReturnValue('   ');

      await command.execute(mockInteraction);

      expect(mockConversationManager.validateMessage).not.toHaveBeenCalled();
      expect(mockConversationManager.buildResumeConversationPayload).toHaveBeenCalledWith(
        expect.any(Object),
        undefined
      );
    });
  });

  describe('error handling', () => {
    it('should handle unexpected errors gracefully', async () => {
      const unexpectedError = new Error('Unexpected error');
      mockConversationManager.extractContextFromInteraction.mockImplementation(() => {
        throw unexpectedError;
      });
      mockGetUserFriendlyErrorMessage.mockReturnValue('An unexpected error occurred');

      await command.execute(mockInteraction);

      expect(mockInteraction.editReply).toHaveBeenCalledWith('An unexpected error occurred');
    });

    it('should handle Discord API errors', async () => {
      const discordError = new Error('Discord API error');
      mockInteraction.deferReply.mockRejectedValue(discordError);
      mockGetUserFriendlyErrorMessage.mockReturnValue('Discord service error');

      await command.execute(mockInteraction);

      expect(mockGetUserFriendlyErrorMessage).toHaveBeenCalledWith(discordError);
    });

    it('should handle conversation not found errors', async () => {
      const notFoundError = new Error('Conversation not found');
      mockBackendApiClient.invokeConversation.mockImplementation(() => {
        throw notFoundError;
      });
      mockGetUserFriendlyErrorMessage.mockReturnValue('No existing conversation found. Use /lenza-new to start a new conversation.');

      await command.execute(mockInteraction);

      expect(mockInteraction.editReply).toHaveBeenCalledWith(
        'No existing conversation found. Use /lenza-new to start a new conversation.'
      );
    });
  });
});