import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SlashCommandBuilder } from 'discord.js';

// Mock all dependencies
vi.mock('../../services/backend-api-client');
vi.mock('../../services/conversation-manager');
vi.mock('../../services/streaming-response-handler');
vi.mock('../../errors/agent-error');

const mockBackendApiClient = {
  invokeNewConversation: vi.fn()
};

const mockConversationManager = {
  extractContextFromInteraction: vi.fn(),
  buildNewConversationPayload: vi.fn(),
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

describe('lenza-new command', () => {
  let command: any;
  let mockInteraction: any;

  beforeEach(async () => {
    // Reset all mocks
    vi.clearAllMocks();
    
    // Import the command after mocks are set up
    command = await import('./lenza-new');
    
    mockInteraction = {
      user: { id: 'user123', username: 'testuser' },
      channelId: 'channel123',
      guildId: 'guild123',
      options: {
        getString: vi.fn().mockReturnValue('Hello Lenza!')
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

    mockConversationManager.buildNewConversationPayload.mockReturnValue({
      serverId: 'guild123',
      channelId: 'channel123',
      memberId: 'user123',
      message: 'Hello Lenza!',
      toolWhitelist: []
    });

    mockConversationManager.convertToBackendRequest.mockReturnValue({
      serverId: 'guild123',
      channelId: 'channel123',
      memberId: 'user123',
      message: 'Hello Lenza!',
      toolWhitelist: []
    });

    mockBackendApiClient.invokeNewConversation.mockReturnValue(async function* () {
      yield { content: 'Hello! How can I help you?', type: 'update' };
    }());
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('command data', () => {
    it('should have correct command structure', () => {
      expect(command.data).toBeInstanceOf(SlashCommandBuilder);
      
      const commandData = command.data.toJSON();
      expect(commandData.name).toBe('lenza-new');
      expect(commandData.description).toBe('Start a new conversation with Lenza');
      
      // Should have a required message option
      const messageOption = commandData.options?.find((opt: any) => opt.name === 'message');
      expect(messageOption).toBeDefined();
      expect(messageOption.required).toBe(true);
      expect(messageOption.type).toBe(3); // STRING type
    });
  });

  describe('execute function', () => {
    it('should execute successfully with valid input', async () => {
      await command.execute(mockInteraction);

      expect(mockInteraction.deferReply).toHaveBeenCalled();
      expect(mockConversationManager.extractContextFromInteraction).toHaveBeenCalledWith(mockInteraction);
      expect(mockConversationManager.validateMessage).toHaveBeenCalledWith('Hello Lenza!');
      expect(mockBackendApiClient.invokeNewConversation).toHaveBeenCalled();
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
      expect(mockBackendApiClient.invokeNewConversation).not.toHaveBeenCalled();
    });

    it('should handle API errors', async () => {
      const apiError = new Error('API unavailable');
      mockBackendApiClient.invokeNewConversation.mockImplementation(() => {
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

    it('should extract message from interaction options', async () => {
      mockInteraction.options.getString.mockReturnValue('Custom message');

      await command.execute(mockInteraction);

      expect(mockInteraction.options.getString).toHaveBeenCalledWith('message');
      expect(mockConversationManager.validateMessage).toHaveBeenCalledWith('Custom message');
    });

    it('should handle missing message option', async () => {
      mockInteraction.options.getString.mockReturnValue(null);

      await command.execute(mockInteraction);

      expect(mockInteraction.editReply).toHaveBeenCalledWith(
        expect.stringContaining('message is required')
      );
    });

    it('should pass correct parameters to conversation manager', async () => {
      await command.execute(mockInteraction);

      expect(mockConversationManager.buildNewConversationPayload).toHaveBeenCalledWith(
        expect.objectContaining({
          serverId: 'guild123',
          channelId: 'channel123',
          memberId: 'user123',
          username: 'testuser'
        }),
        'Hello Lenza!',
        [] // Empty tool whitelist for new conversations
      );
    });

    it('should create streaming response handler with correct interaction', async () => {
      const { StreamingResponseHandler } = await import('../../services/streaming-response-handler');
      
      await command.execute(mockInteraction);

      expect(StreamingResponseHandler).toHaveBeenCalledWith(mockInteraction);
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
  });
});