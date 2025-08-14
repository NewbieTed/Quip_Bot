import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { StreamingResponseHandler } from '../../services/streaming-response-handler';
import { BackendApiClient } from '../../services/backend-api-client';

// Mock dependencies
vi.mock('../../services/tool-approval-handler');
vi.mock('../../config/bot-config', () => ({
  getBotConfig: vi.fn(() => ({
    maxMessageLength: 2000,
    approvalTimeoutSeconds: 60
  }))
}));

global.fetch = vi.fn();

describe('Streaming Performance Tests', () => {
  let mockInteraction: any;
  let mockFetch: any;

  beforeEach(() => {
    mockFetch = vi.mocked(fetch);
    mockFetch.mockClear();

    mockInteraction = {
      user: { id: 'user123', username: 'testuser' },
      channelId: 'channel123',
      guildId: 'guild123',
      editReply: vi.fn().mockResolvedValue({}),
      followUp: vi.fn().mockResolvedValue({})
    };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('High-frequency streaming updates', () => {
    it('should handle rapid streaming updates efficiently', async () => {
      const handler = new StreamingResponseHandler(mockInteraction);
      
      // Create a stream with many rapid updates
      const mockStream = async function* () {
        for (let i = 0; i < 100; i++) {
          yield { content: `Update ${i}`, type: 'update' as const };
        }
      };

      const startTime = performance.now();
      await handler.handleStream(mockStream());
      const endTime = performance.now();

      const duration = endTime - startTime;
      
      // Should complete within reasonable time (less than 1 second for 100 updates)
      expect(duration).toBeLessThan(1000);
      
      // Should have called editReply for each update
      expect(mockInteraction.editReply).toHaveBeenCalledTimes(100);
      expect(mockInteraction.editReply).toHaveBeenLastCalledWith('Update 99');
    });

    it('should handle large content updates efficiently', async () => {
      const handler = new StreamingResponseHandler(mockInteraction);
      
      // Create a stream with large content updates
      const largeContent = 'a'.repeat(1500); // Large but within Discord limits
      const mockStream = async function* () {
        for (let i = 0; i < 10; i++) {
          yield { content: `${largeContent} - Update ${i}`, type: 'update' as const };
        }
      };

      const startTime = performance.now();
      await handler.handleStream(mockStream());
      const endTime = performance.now();

      const duration = endTime - startTime;
      
      // Should complete within reasonable time
      expect(duration).toBeLessThan(500);
      
      expect(mockInteraction.editReply).toHaveBeenCalledTimes(10);
    });

    it('should handle mixed update types efficiently', async () => {
      const handler = new StreamingResponseHandler(mockInteraction);
      
      const mockStream = async function* () {
        for (let i = 0; i < 50; i++) {
          if (i % 3 === 0) {
            yield { content: `Progress ${i}`, type: 'progress' as const };
          } else {
            yield { content: `Update ${i}`, type: 'update' as const };
          }
        }
      };

      const startTime = performance.now();
      await handler.handleStream(mockStream());
      const endTime = performance.now();

      const duration = endTime - startTime;
      
      expect(duration).toBeLessThan(500);
      expect(mockInteraction.editReply).toHaveBeenCalledTimes(50);
    });
  });

  describe('Memory usage during streaming', () => {
    it('should not accumulate memory during long streams', async () => {
      const handler = new StreamingResponseHandler(mockInteraction);
      
      // Create a very long stream to test memory usage
      const mockStream = async function* () {
        for (let i = 0; i < 1000; i++) {
          yield { content: `Long stream update ${i}`, type: 'update' as const };
          
          // Yield control to allow garbage collection
          if (i % 100 === 0) {
            await new Promise(resolve => setTimeout(resolve, 0));
          }
        }
      };

      const initialMemory = process.memoryUsage().heapUsed;
      await handler.handleStream(mockStream());
      
      // Force garbage collection if available
      if (global.gc) {
        global.gc();
      }
      
      const finalMemory = process.memoryUsage().heapUsed;
      const memoryIncrease = finalMemory - initialMemory;
      
      // Memory increase should be reasonable (less than 10MB)
      expect(memoryIncrease).toBeLessThan(10 * 1024 * 1024);
    });
  });

  describe('Concurrent streaming performance', () => {
    it('should handle multiple concurrent streams efficiently', async () => {
      const numConcurrentStreams = 10;
      const updatesPerStream = 20;
      
      const handlers = Array.from({ length: numConcurrentStreams }, () => {
        const interaction = {
          ...mockInteraction,
          editReply: vi.fn().mockResolvedValue({})
        };
        return new StreamingResponseHandler(interaction);
      });

      const createMockStream = (streamId: number) => async function* () {
        for (let i = 0; i < updatesPerStream; i++) {
          yield { content: `Stream ${streamId} - Update ${i}`, type: 'update' as const };
        }
      };

      const startTime = performance.now();
      
      await Promise.all(
        handlers.map((handler, index) => 
          handler.handleStream(createMockStream(index)())
        )
      );
      
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      // Should complete all streams within reasonable time
      expect(duration).toBeLessThan(2000);
      
      // Verify all handlers processed their updates
      handlers.forEach((handler, index) => {
        const interaction = (handler as any).interaction;
        expect(interaction.editReply).toHaveBeenCalledTimes(updatesPerStream);
        expect(interaction.editReply).toHaveBeenLastCalledWith(
          `Stream ${index} - Update ${updatesPerStream - 1}`
        );
      });
    });
  });

  describe('API client streaming performance', () => {
    it('should parse streaming responses efficiently', async () => {
      const client = new BackendApiClient();
      
      // Mock a response with many chunks
      const chunks = Array.from({ length: 100 }, (_, i) => 
        `data: {"content":"Update ${i}","type":"update"}\n\n`
      );
      
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => {
            let chunkIndex = 0;
            return {
              read: vi.fn().mockImplementation(() => {
                if (chunkIndex < chunks.length) {
                  const chunk = chunks[chunkIndex++];
                  return Promise.resolve({
                    done: false,
                    value: new TextEncoder().encode(chunk)
                  });
                } else {
                  return Promise.resolve({ done: true, value: undefined });
                }
              })
            };
          }
        }
      };
      
      mockFetch.mockResolvedValue(mockResponse);

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Test message',
        toolWhitelist: []
      };

      const startTime = performance.now();
      
      const generator = client.invokeNewConversation(request);
      const results = [];
      
      for await (const response of generator) {
        results.push(response);
      }
      
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      // Should parse all chunks efficiently
      expect(duration).toBeLessThan(500);
      expect(results).toHaveLength(100);
      expect(results[0]).toEqual({ content: 'Update 0', type: 'update' });
      expect(results[99]).toEqual({ content: 'Update 99', type: 'update' });
    });

    it('should handle malformed streaming data gracefully', async () => {
      const client = new BackendApiClient();
      
      // Mix of valid and invalid chunks
      const chunks = [
        'data: {"content":"Valid 1","type":"update"}\n\n',
        'data: invalid json\n\n',
        'data: {"content":"Valid 2","type":"update"}\n\n',
        'invalid line format',
        'data: {"content":"Valid 3","type":"update"}\n\n'
      ];
      
      const mockResponse = {
        ok: true,
        body: {
          getReader: () => {
            let chunkIndex = 0;
            return {
              read: vi.fn().mockImplementation(() => {
                if (chunkIndex < chunks.length) {
                  const chunk = chunks[chunkIndex++];
                  return Promise.resolve({
                    done: false,
                    value: new TextEncoder().encode(chunk)
                  });
                } else {
                  return Promise.resolve({ done: true, value: undefined });
                }
              })
            };
          }
        }
      };
      
      mockFetch.mockResolvedValue(mockResponse);

      const request = {
        serverId: 'server123',
        channelId: 'channel123',
        memberId: 'member123',
        message: 'Test message',
        toolWhitelist: []
      };

      const generator = client.invokeNewConversation(request);
      const results = [];
      
      // Should handle errors gracefully and continue processing
      try {
        for await (const response of generator) {
          results.push(response);
        }
      } catch (error) {
        // Expected to throw on invalid JSON
      }
      
      // Should have processed at least some valid responses before error
      expect(results.length).toBeGreaterThan(0);
      expect(results[0]).toEqual({ content: 'Valid 1', type: 'update' });
    });
  });

  describe('Message splitting performance', () => {
    it('should split very long messages efficiently', async () => {
      const handler = new StreamingResponseHandler(mockInteraction);
      
      // Create a message that will require multiple splits
      const veryLongContent = 'a'.repeat(10000);
      
      const startTime = performance.now();
      await handler.splitLongMessage(veryLongContent);
      const endTime = performance.now();
      
      const duration = endTime - startTime;
      
      // Should split efficiently
      expect(duration).toBeLessThan(100);
      
      // Should have called editReply and followUp
      expect(mockInteraction.editReply).toHaveBeenCalled();
      expect(mockInteraction.followUp).toHaveBeenCalled();
    });

    it('should handle multiple rapid message splits', async () => {
      const handler = new StreamingResponseHandler(mockInteraction);
      
      const longMessages = Array.from({ length: 10 }, (_, i) => 
        `Message ${i}: ${'a'.repeat(3000)}`
      );
      
      const startTime = performance.now();
      
      for (const message of longMessages) {
        await handler.splitLongMessage(message);
      }
      
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      // Should handle all splits efficiently
      expect(duration).toBeLessThan(500);
      
      // Should have made appropriate Discord API calls
      expect(mockInteraction.editReply).toHaveBeenCalledTimes(10);
      expect(mockInteraction.followUp).toHaveBeenCalled();
    });
  });
});