import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ToolApprovalHandler } from './tool-approval-handler';
import { getBotConfig } from '../config/bot-config';

// Mock dependencies
vi.mock('../config/bot-config', () => ({
  getBotConfig: vi.fn(() => ({
    approvalTimeoutSeconds: 60
  }))
}));

// Mock Discord.js components
const mockActionRowBuilder = {
  addComponents: vi.fn().mockReturnThis()
};

const mockButtonBuilder = {
  setCustomId: vi.fn().mockReturnThis(),
  setLabel: vi.fn().mockReturnThis(),
  setStyle: vi.fn().mockReturnThis(),
  setEmoji: vi.fn().mockReturnThis()
};

vi.mock('discord.js', () => ({
  ActionRowBuilder: vi.fn(() => mockActionRowBuilder),
  ButtonBuilder: vi.fn(() => mockButtonBuilder),
  ButtonStyle: {
    Success: 3,
    Danger: 4,
    Primary: 1
  }
}));

describe('ToolApprovalHandler', () => {
  let handler: ToolApprovalHandler;
  let mockInteraction: any;

  beforeEach(() => {
    mockInteraction = {
      user: { id: 'user123', username: 'testuser' },
      channelId: 'channel123',
      guildId: 'guild123',
      editReply: vi.fn().mockResolvedValue({
        createMessageComponentCollector: vi.fn()
      }),
      followUp: vi.fn().mockResolvedValue({})
    };

    handler = new ToolApprovalHandler();
    vi.clearAllTimers();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  describe('constructor', () => {
    it('should initialize with empty pending approvals', () => {
      expect(handler).toBeInstanceOf(ToolApprovalHandler);
      expect(getBotConfig).toHaveBeenCalled();
    });
  });

  describe('requestApproval', () => {
    it('should create approval request with buttons', async () => {
      const mockCollector = {
        on: vi.fn(),
        stop: vi.fn()
      };
      
      const mockMessage = {
        createMessageComponentCollector: vi.fn().mockReturnValue(mockCollector)
      };
      
      mockInteraction.editReply.mockResolvedValue(mockMessage);

      const approvalRequest = {
        userId: 'user123',
        toolName: 'test_tool',
        description: 'Test tool description',
        originalInteraction: mockInteraction
      };

      // Start the approval request (don't await yet)
      const approvalPromise = handler.requestApproval(approvalRequest);

      // Verify the message was sent with buttons
      expect(mockInteraction.editReply).toHaveBeenCalledWith({
        content: expect.stringContaining('test_tool'),
        components: expect.arrayContaining([mockActionRowBuilder])
      });

      // Verify collector was created
      expect(mockMessage.createMessageComponentCollector).toHaveBeenCalledWith({
        time: 60000
      });

      // Simulate timeout
      vi.advanceTimersByTime(60000);
      
      const result = await approvalPromise;
      expect(result.timedOut).toBe(true);
      expect(result.approved).toBe(false);
    });

    it('should handle approval button click', async () => {
      const mockCollector = {
        on: vi.fn(),
        stop: vi.fn()
      };
      
      const mockMessage = {
        createMessageComponentCollector: vi.fn().mockReturnValue(mockCollector)
      };
      
      mockInteraction.editReply.mockResolvedValue(mockMessage);

      const approvalRequest = {
        userId: 'user123',
        toolName: 'test_tool',
        description: 'Test tool description',
        originalInteraction: mockInteraction
      };

      const approvalPromise = handler.requestApproval(approvalRequest);

      // Get the collector callback
      const collectCallback = mockCollector.on.mock.calls.find(call => call[0] === 'collect')[1];
      
      // Simulate approval button click
      const mockButtonInteraction = {
        user: { id: 'user123' },
        customId: expect.stringContaining('approve'),
        update: vi.fn().mockResolvedValue({})
      };

      collectCallback(mockButtonInteraction);

      const result = await approvalPromise;
      expect(result.approved).toBe(true);
      expect(result.addToWhitelist).toBe(false);
      expect(result.timedOut).toBe(false);
    });

    it('should handle deny button click', async () => {
      const mockCollector = {
        on: vi.fn(),
        stop: vi.fn()
      };
      
      const mockMessage = {
        createMessageComponentCollector: vi.fn().mockReturnValue(mockCollector)
      };
      
      mockInteraction.editReply.mockResolvedValue(mockMessage);

      const approvalRequest = {
        userId: 'user123',
        toolName: 'test_tool',
        description: 'Test tool description',
        originalInteraction: mockInteraction
      };

      const approvalPromise = handler.requestApproval(approvalRequest);

      // Get the collector callback
      const collectCallback = mockCollector.on.mock.calls.find(call => call[0] === 'collect')[1];
      
      // Simulate deny button click
      const mockButtonInteraction = {
        user: { id: 'user123' },
        customId: expect.stringContaining('deny'),
        update: vi.fn().mockResolvedValue({})
      };

      collectCallback(mockButtonInteraction);

      const result = await approvalPromise;
      expect(result.approved).toBe(false);
      expect(result.addToWhitelist).toBe(false);
      expect(result.timedOut).toBe(false);
    });

    it('should handle approve and trust button click', async () => {
      const mockCollector = {
        on: vi.fn(),
        stop: vi.fn()
      };
      
      const mockMessage = {
        createMessageComponentCollector: vi.fn().mockReturnValue(mockCollector)
      };
      
      mockInteraction.editReply.mockResolvedValue(mockMessage);

      const approvalRequest = {
        userId: 'user123',
        toolName: 'test_tool',
        description: 'Test tool description',
        originalInteraction: mockInteraction
      };

      const approvalPromise = handler.requestApproval(approvalRequest);

      // Get the collector callback
      const collectCallback = mockCollector.on.mock.calls.find(call => call[0] === 'collect')[1];
      
      // Simulate approve and trust button click
      const mockButtonInteraction = {
        user: { id: 'user123' },
        customId: expect.stringContaining('trust'),
        update: vi.fn().mockResolvedValue({})
      };

      collectCallback(mockButtonInteraction);

      const result = await approvalPromise;
      expect(result.approved).toBe(true);
      expect(result.addToWhitelist).toBe(true);
      expect(result.timedOut).toBe(false);
    });

    it('should reject clicks from wrong user', async () => {
      const mockCollector = {
        on: vi.fn(),
        stop: vi.fn()
      };
      
      const mockMessage = {
        createMessageComponentCollector: vi.fn().mockReturnValue(mockCollector)
      };
      
      mockInteraction.editReply.mockResolvedValue(mockMessage);

      const approvalRequest = {
        userId: 'user123',
        toolName: 'test_tool',
        description: 'Test tool description',
        originalInteraction: mockInteraction
      };

      const approvalPromise = handler.requestApproval(approvalRequest);

      // Get the collector callback
      const collectCallback = mockCollector.on.mock.calls.find(call => call[0] === 'collect')[1];
      
      // Simulate button click from wrong user
      const mockButtonInteraction = {
        user: { id: 'wrong_user' },
        customId: expect.stringContaining('approve'),
        reply: vi.fn().mockResolvedValue({})
      };

      collectCallback(mockButtonInteraction);

      // Should send ephemeral message to wrong user
      expect(mockButtonInteraction.reply).toHaveBeenCalledWith({
        content: expect.stringContaining('only the original requester'),
        ephemeral: true
      });

      // Approval should still be pending
      vi.advanceTimersByTime(60000);
      const result = await approvalPromise;
      expect(result.timedOut).toBe(true);
    });
  });

  describe('createApprovalButtons', () => {
    it('should create three buttons with correct properties', () => {
      const approvalId = 'test_approval_123';
      const buttons = handler.createApprovalButtons(approvalId);

      expect(mockButtonBuilder.setCustomId).toHaveBeenCalledWith(`tool_approval_approve_${approvalId}`);
      expect(mockButtonBuilder.setCustomId).toHaveBeenCalledWith(`tool_approval_deny_${approvalId}`);
      expect(mockButtonBuilder.setCustomId).toHaveBeenCalledWith(`tool_approval_trust_${approvalId}`);
      
      expect(mockButtonBuilder.setLabel).toHaveBeenCalledWith('Approve');
      expect(mockButtonBuilder.setLabel).toHaveBeenCalledWith('Deny');
      expect(mockButtonBuilder.setLabel).toHaveBeenCalledWith('Approve & Trust');
    });
  });

  describe('cleanupExpiredApprovals', () => {
    it('should remove expired approvals', () => {
      // Add a mock approval that should be cleaned up
      const expiredApproval = {
        userId: 'user123',
        toolName: 'test_tool',
        timestamp: new Date(Date.now() - 120000), // 2 minutes ago
        timeoutId: setTimeout(() => {}, 1000)
      };

      // Access private property for testing
      (handler as any).pendingApprovals.set('expired_id', expiredApproval);

      handler.cleanupExpiredApprovals();

      // Should have been removed
      expect((handler as any).pendingApprovals.has('expired_id')).toBe(false);
    });

    it('should keep non-expired approvals', () => {
      // Add a mock approval that should not be cleaned up
      const activeApproval = {
        userId: 'user123',
        toolName: 'test_tool',
        timestamp: new Date(), // Current time
        timeoutId: setTimeout(() => {}, 1000)
      };

      // Access private property for testing
      (handler as any).pendingApprovals.set('active_id', activeApproval);

      handler.cleanupExpiredApprovals();

      // Should still be there
      expect((handler as any).pendingApprovals.has('active_id')).toBe(true);
    });
  });

  describe('generateApprovalId', () => {
    it('should generate unique approval IDs', () => {
      const id1 = (handler as any).generateApprovalId();
      const id2 = (handler as any).generateApprovalId();
      
      expect(id1).not.toBe(id2);
      expect(typeof id1).toBe('string');
      expect(typeof id2).toBe('string');
    });
  });
});