/**
 * Bot configuration interface and default values
 */
export interface BotConfiguration {
  backendApiUrl: string;
  backendApiTimeout: number;
  maxMessageLength: number;
  approvalTimeoutSeconds: number;
  retryAttempts: number;
  retryDelayMs: number;
}

/**
 * Default configuration values
 */
const DEFAULT_CONFIG: BotConfiguration = {
  backendApiUrl: process.env.BACKEND_URL || 'http://localhost:8080',
  backendApiTimeout: parseInt(process.env.BACKEND_TIMEOUT || '30000'),
  maxMessageLength: 2000, // Discord message limit
  approvalTimeoutSeconds: 60,
  retryAttempts: 3,
  retryDelayMs: 1000
};

/**
 * Get bot configuration with environment variable overrides
 */
export function getBotConfig(): BotConfiguration {
  return {
    backendApiUrl: process.env.BACKEND_URL || DEFAULT_CONFIG.backendApiUrl,
    backendApiTimeout: parseInt(process.env.BACKEND_TIMEOUT || DEFAULT_CONFIG.backendApiTimeout.toString()),
    maxMessageLength: parseInt(process.env.MAX_MESSAGE_LENGTH || DEFAULT_CONFIG.maxMessageLength.toString()),
    approvalTimeoutSeconds: parseInt(process.env.APPROVAL_TIMEOUT || DEFAULT_CONFIG.approvalTimeoutSeconds.toString()),
    retryAttempts: parseInt(process.env.RETRY_ATTEMPTS || DEFAULT_CONFIG.retryAttempts.toString()),
    retryDelayMs: parseInt(process.env.RETRY_DELAY_MS || DEFAULT_CONFIG.retryDelayMs.toString())
  };
}