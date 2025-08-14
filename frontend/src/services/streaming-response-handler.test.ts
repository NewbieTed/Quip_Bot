import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { StreamingResponseHandler } from './streaming-response-handler';
import { ToolApprovalHandler } from './tool-approval-handler';
import { AgentError } from '../errors/agent-error';

// Mock dependencies
vi.mock('./tool-approval-handler');
vi.mock('../config/bot-config', () => ({
  getBotConfig: vi.fn(() => ({
    maxMessageLength: 2000,
    approvalTimeoutSeconds: 60
  }))
}));

describe('StreamingResponseHandler', () => {
  let handler: StreamingResponseHandler;
  let mockInteraction: any;
  let mockToolApprovalHandler: any;

  beforeEach(() => {
    mockInteraction = {
      user: { id: 'user123', username: 'testuser' },
      channelId: 'channel123',
      guildId: 'guild123',
      editReply: vi.fn().mockResolvedValue({}),
      followUp: vi.fn().mockResolvedValue({}),
      replied: false,
      deferred: false
    };

    mockToolApprovalHandler = {
      requestApproval: vi.fn(),
      handleApprovalResponse: vi.fn(),
      cleanupExpiredApprovals: vi.fn()
    };

    vi.mocked(ToolApprovalHandler).mockImplementation(() => mockToolApprovalHandler);

    handler = new StreamingResponseHandler(mockInteraction);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('constructor', () => {
    it('should initialize with correct properties', () => {
      expect(handler).toBeInstanceOf(StreamingResponseHandler);
      expect(ToolApprovalHandler).toHaveBeenCalled();
    });
  });

  describe('handleStream', () => {
    it('should handle streaming responses correctly', async () => {
      const mockStream = async function* () {
        yield { content: 'Hello', type: 'update' as const };
        yield { content: 'Hello World', type: 'update' as const };
        yield { content: 'Hello World!', type: 'update' as const };
      };

      await handler.handleStream(mockStream());

      expect(mockInteraction.editReply).toHaveBeenCalledTimes(3);
      expect(mockInteraction.editReply).toHaveBeenLastCalledWith('Hello World!');
    });

    it('should handle progress messages', async () => {
      const mockStream = async function* () {
        yield { content: 'Processing...', type: 'progress' as const };
        yield { content: 'Final result', type: 'update' as const };
      };

      await handler.handleStream(mockStream());

      expect(mockInteraction.editReply).toHaveBeenCalledWith('🔄 Processing...');
      expect(mockInteraction.editReply).toHaveBeenCalledWith('Final result');
    });

    it('should handle tool approval requests', async () => {
      mockToolApprovalHandler.requestApproval.mockResolvedValue({
        approved: true,
        addToWhitelist: false,
        timedOut: false
      });

      const mockStream = async function* () {
        yield { content: 'Tool approval needed', type: 'interrupt' as const, tool_name: 'test_tool' };
        yield { content: 'Tool approved, continuing...', type: 'update' as const };
      };

      await handler.handleStream(mockStream());

      expect(mockToolApprovalHandler.requestApproval).toHaveBeenCalledWith({
        userId: 'user123',
        toolName: 'test_tool',
        description: 'Tool approval needed',
        originalInteraction: mockInteraction
      });
    });

    it('should handle tool approval denial', async () => {
      mockToolApprovalHandler.requestApproval.mockResolvedValue({
        approved: false,
        addToWhitelist: false,
        timedOut: false
      });

      const mockStream = async function* () {
        yield { content: 'Tool approval needed', type: 'interrupt' as const, tool_name: 'test_tool' };
      };

      await handler.handleStream(mockStream());

      expect(mockInteraction.editReply).toHaveBeenCalledWith(
        expect.stringContaining('Tool request denied')
      );
    });

    it('should handle tool approval timeout', async () => {
      mockToolApprovalHandler.requestApproval.mockResolvedValue({
        approved: false,
        addToWhitelist: false,
        timedOut: true
      });

      const mockStream = async function* () {
        yield { content: 'Tool approval needed', type: 'interrupt' as const, tool_name: 'test_tool' };
      };

      await handler.handleStream(mockStream());

      expect(mockInteraction.editReply).toHaveBeenCalledWith(
        expect.stringContaining('Tool approval timed out')
      );
    });

    it('should handle stream errors gracefully', async () => {
      const mockStream = async function* () {
        yield { content: 'Starting...', type: 'update' as const };
        throw new AgentError('network', 'Connection lost');
      };

      await expect(handler.handleStream(mockStream())).rejects.toThrow(AgentError);
      expect(mockInteraction.editReply).toHaveBeenCalledWith('Starting...');
    });
  });

  describe('splitLongMessage', () => {
    it('should split messages longer than max length', async () => {
      const longMessage = 'a'.repeat(3000);
      
      await handler.splitLongMessage(longMessage);

      expect(mockInteraction.editReply).toHaveBeenCalledWith(
        expect.stringContaining('a'.repeat(1900)) // Should be truncated
      );
      expect(mockInteraction.followUp).toHaveBeenCalledWith(
        expect.stringContaining('a'.repeat(1000)) // Remaining content
      );
    });

    it('should not split messages within max length', async () => {
      const shortMessage = 'Short message';
      
      await handler.splitLongMessage(shortMessage);

      expect(mockInteraction.editReply).toHaveBeenCalledWith(shortMessage);
      expect(mockInteraction.followUp).not.toHaveBeenCalled();
    });
  });

  describe('formatProgressMessage', () => {
    it('should format progress messages with spinner', () => {
      const result = handler.formatProgressMessage('Processing...');
      expect(result).toBe('🔄 Processing...');
    });

    it('should handle empty progress messages', () => {
      const result = handler.formatProgressMessage('');
      expect(result).toBe('🔄 Processing...');
    });
  });

  describe('updateMessage', () => {
    it('should update message via editReply', async () => {
      await handler.updateMessage('Test message');
      
      expect(mockInteraction.editReply).toHaveBeenCalledWith('Test message');
    });

    it('should handle update errors gracefully', async () => {
      mockInteraction.editReply.mockRejectedValue(new Error('Discord API error'));
      
      await expect(handler.updateMessage('Test message')).rejects.toThrow();
    });
  });
});