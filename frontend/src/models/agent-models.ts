/**
 * Data models for agent API requests and responses
 */

/**
 * Streaming response types from the agent
 */
export type ResponseType = 'progress' | 'update' | 'interrupt' | 'unknown';

/**
 * Streaming response from the agent
 */
export interface StreamingResponse {
  content: string;
  type: ResponseType;
  tool_name?: string;
}

/**
 * Request for invoking agent with existing conversation
 */
export interface AgentRequest {
  serverId: string;
  channelId: string;
  memberId: string;
  message?: string;
  approved?: boolean;
  toolWhitelistUpdate?: string[];
}

/**
 * Request for starting new conversation with agent
 */
export interface NewAgentRequest {
  serverId: string;
  channelId: string;
  memberId: string;
  message: string;
  whitelistedTools?: string[];
}

/**
 * Backend API request format (matches AssistantRequestDto)
 */
export interface BackendAgentRequest {
  message: string;
  channelId: number;
  memberId: number;
}

/**
 * Tool approval request data
 */
export interface ToolApprovalData {
  toolName: string;
  description: string;
  userId: string;
  timestamp: Date;
  timeoutId: NodeJS.Timeout;
}

/**
 * Tool approval request with interaction context
 */
export interface ToolApprovalRequest {
  approvalId: string;
  userId: string;
  toolName: string;
  description: string;
  originalInteraction: import('discord.js').ChatInputCommandInteraction;
  timeoutId: NodeJS.Timeout;
  resolve: (result: ApprovalResult) => void;
}

/**
 * Tool approval result
 */
export interface ApprovalResult {
  approved: boolean;
  addToWhitelist: boolean;
  timedOut: boolean;
  error?: string;
}

/**
 * Tool whitelist update request
 */
export interface ToolWhitelistRequest {
  memberId: number;
  channelId: number;
  addRequests?: ToolWhitelistAddRequest[];
  removeRequests?: ToolWhitelistRemoveRequest[];
}

/**
 * Tool whitelist add request
 */
export interface ToolWhitelistAddRequest {
  toolName: string;
  scope: string; // 'GLOBAL', 'SERVER', or 'CONVERSATION'
  agentConversationId?: number | null;
  expiresAt?: string | null; // ISO date string
}

/**
 * Tool whitelist remove request
 */
export interface ToolWhitelistRemoveRequest {
  toolName: string;
  scope: string; // 'GLOBAL', 'SERVER', or 'CONVERSATION'
  agentConversationId?: number | null;
}

/**
 * Tool whitelist response
 */
export interface ToolWhitelistResponse {
  success: boolean;
  message?: string;
}

/**
 * Re-export conversation management types
 */
export type {
  ConversationContext,
  NewConversationPayload,
  ResumeConversationPayload,
  BackendConversationRequest
} from '../services/conversation-manager';