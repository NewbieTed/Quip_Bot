import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ConversationManager } from './conversation-manager';
import { AgentError } from '../errors/agent-error';

describe('ConversationManager', () => {
  let manager: ConversationManager;

  beforeEach(() => {
    manager = new ConversationManager();
  });

  describe('createConversationContext', () => {
    it('should create valid conversation context', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser',
        'test-channel',
        'test-guild'
      );

      expect(context).toEqual({
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        username: 'testuser',
        channelName: 'test-channel',
        guildName: 'test-guild'
      });
    });

    it('should handle missing optional parameters', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser'
      );

      expect(context.serverId).toBe('server123');
      expect(context.channelId).toBe('channel123');
      expect(context.memberId).toBe('member123');
      expect(context.username).toBe('testuser');
      expect(context.channelName).toBeUndefined();
      expect(context.guildName).toBeUndefined();
    });
  });

  describe('buildNewConversationPayload', () => {
    it('should build correct payload for new conversation', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser',
        'test-channel',
        'test-guild'
      );

      const payload = manager.buildNewConversationPayload(context, 'Hello Lenza!', ['tool1', 'tool2']);

      expect(payload).toEqual({
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Hello Lenza!',
        toolWhitelist: ['tool1', 'tool2']
      });
    });

    it('should use empty tool whitelist by default', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser'
      );

      const payload = manager.buildNewConversationPayload(context, 'Hello Lenza!');

      expect(payload.toolWhitelist).toEqual([]);
    });
  });

  describe('buildResumeConversationPayload', () => {
    it('should build correct payload for resume conversation', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser'
      );

      const payload = manager.buildResumeConversationPayload(context, 'Continue conversation');

      expect(payload).toEqual({
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Continue conversation'
      });
    });

    it('should handle optional message parameter', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser'
      );

      const payload = manager.buildResumeConversationPayload(context);

      expect(payload.message).toBeUndefined();
    });
  });

  describe('convertToBackendRequest', () => {
    it('should convert new conversation payload correctly', () => {
      const payload = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Hello Lenza!',
        toolWhitelist: ['tool1', 'tool2']
      };

      const backendRequest = manager.convertToBackendRequest(payload);

      expect(backendRequest).toEqual({
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Hello Lenza!',
        toolWhitelist: ['tool1', 'tool2']
      });
    });

    it('should convert resume conversation payload correctly', () => {
      const payload = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Continue conversation'
      };

      const backendRequest = manager.convertToBackendRequest(payload);

      expect(backendRequest).toEqual({
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Continue conversation'
      });
    });
  });

  describe('validateConversationContext', () => {
    it('should validate correct context', () => {
      const context = manager.createConversationContext(
        'server123',
        'channel123',
        'member123',
        'testuser'
      );

      expect(() => manager.validateConversationContext(context)).not.toThrow();
    });

    it('should throw error for missing serverId', () => {
      const context = {
        serverId: '',
        channelId: 'channel123',
        memberId: 'member123',
        username: 'testuser'
      };

      expect(() => manager.validateConversationContext(context)).toThrow(AgentError);
    });

    it('should throw error for missing channelId', () => {
      const context = {
        serverId: 'server123',
        channelId: '',
        memberId: 'member123',
        username: 'testuser'
      };

      expect(() => manager.validateConversationContext(context)).toThrow(AgentError);
    });

    it('should throw error for missing memberId', () => {
      const context = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: '',
        username: 'testuser'
      };

      expect(() => manager.validateConversationContext(context)).toThrow(AgentError);
    });

    it('should throw error for missing username', () => {
      const context = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        username: ''
      };

      expect(() => manager.validateConversationContext(context)).toThrow(AgentError);
    });
  });

  describe('validateMessage', () => {
    it('should validate correct message', () => {
      expect(() => manager.validateMessage('Hello Lenza!')).not.toThrow();
    });

    it('should throw error for empty message', () => {
      expect(() => manager.validateMessage('')).toThrow(AgentError);
    });

    it('should throw error for whitespace-only message', () => {
      expect(() => manager.validateMessage('   ')).toThrow(AgentError);
    });

    it('should throw error for message too long', () => {
      const longMessage = 'a'.repeat(2001);
      expect(() => manager.validateMessage(longMessage)).toThrow(AgentError);
    });

    it('should accept message at max length', () => {
      const maxMessage = 'a'.repeat(2000);
      expect(() => manager.validateMessage(maxMessage)).not.toThrow();
    });
  });

  describe('validateToolWhitelist', () => {
    it('should validate correct tool whitelist', () => {
      expect(() => manager.validateToolWhitelist(['tool1', 'tool2'])).not.toThrow();
    });

    it('should validate empty tool whitelist', () => {
      expect(() => manager.validateToolWhitelist([])).not.toThrow();
    });

    it('should throw error for non-array input', () => {
      expect(() => manager.validateToolWhitelist('not-array' as any)).toThrow(AgentError);
    });

    it('should throw error for array with non-string elements', () => {
      expect(() => manager.validateToolWhitelist(['tool1', 123] as any)).toThrow(AgentError);
    });

    it('should throw error for array with empty string elements', () => {
      expect(() => manager.validateToolWhitelist(['tool1', ''])).toThrow(AgentError);
    });
  });

  describe('extractContextFromInteraction', () => {
    it('should extract context from Discord interaction', () => {
      const mockInteraction = {
        user: { id: 'user123', username: 'testuser' },
        channelId: 'channel123',
        guildId: 'guild123',
        channel: { name: 'test-channel' },
        guild: { name: 'test-guild' }
      };

      const context = manager.extractContextFromInteraction(mockInteraction as any);

      expect(context).toEqual({
        serverId: 'guild123',
        channelId: 'channel123',
        memberId: 'user123',
        username: 'testuser',
        channelName: 'test-channel',
        guildName: 'test-guild'
      });
    });

    it('should handle missing optional properties', () => {
      const mockInteraction = {
        user: { id: 'user123', username: 'testuser' },
        channelId: 'channel123',
        guildId: 'guild123'
      };

      const context = manager.extractContextFromInteraction(mockInteraction as any);

      expect(context.serverId).toBe('guild123');
      expect(context.channelId).toBe('channel123');
      expect(context.memberId).toBe('user123');
      expect(context.username).toBe('testuser');
      expect(context.channelName).toBeUndefined();
      expect(context.guildName).toBeUndefined();
    });
  });
});