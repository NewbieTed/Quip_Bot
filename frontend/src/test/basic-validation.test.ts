import { describe, it, expect } from 'vitest';

describe('Basic Test Environment Validation', () => {
  it('should run basic tests', () => {
    expect(1 + 1).toBe(2);
  });

  it('should handle async operations', async () => {
    const result = await Promise.resolve('test');
    expect(result).toBe('test');
  });

  it('should handle error throwing', () => {
    expect(() => {
      throw new Error('test error');
    }).toThrow('test error');
  });

  it('should validate environment setup', () => {
    expect(typeof process).toBe('object');
    expect(typeof global).toBe('object');
  });
});