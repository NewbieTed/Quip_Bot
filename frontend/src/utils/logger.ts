import * as fs from 'fs';
import * as path from 'path';
import { AgentError } from '../errors/agent-error';

/**
 * Log levels for filtering and categorization
 */
export type LogLevel = 'debug' | 'info' | 'warn' | 'error' | 'critical';

/**
 * Structured log entry interface
 */
interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  context?: Record<string, any>;
  error?: {
    type: string;
    message: string;
    stack?: string;
    severity?: string;
  };
  performance?: {
    duration?: number;
    operation?: string;
  };
  user?: {
    id?: string;
    username?: string;
    channel?: string;
    guild?: string;
  };
}

/**
 * Enhanced logger with structured logging and privacy protection
 */
class Logger {
  private logDir: string;
  private logFile: string;
  private errorLogFile: string;
  private currentLogLevel: LogLevel;
  private maxLogFileSize: number = 10 * 1024 * 1024; // 10MB
  private maxLogFiles: number = 5;

  constructor() {
    this.logDir = path.join(__dirname, '../../logs');
    this.logFile = path.join(this.logDir, 'frontend.log');
    this.errorLogFile = path.join(this.logDir, 'frontend-error.log');
    this.currentLogLevel = (process.env.LOG_LEVEL as LogLevel) || 'info';
    
    // Ensure logs directory exists
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir, { recursive: true });
    }

    // Rotate logs if they're too large
    this.rotateLogs();
  }

  /**
   * Log debug message
   */
  debug(message: string, context?: Record<string, any>): void {
    this.log('debug', message, context);
  }

  /**
   * Log info message
   */
  info(message: string, context?: Record<string, any>): void {
    this.log('info', message, context);
  }

  /**
   * Log warning message
   */
  warn(message: string, context?: Record<string, any>): void {
    this.log('warn', message, context);
  }

  /**
   * Log error message
   */
  error(message: string, error?: Error | AgentError, context?: Record<string, any>): void {
    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level: 'error',
      message: this.sanitizeMessage(message),
      context: this.sanitizeContext(context)
    };

    if (error) {
      if (error instanceof AgentError) {
        logEntry.error = {
          type: error.type,
          message: error.getSanitizedMessage(),
          severity: error.severity,
          stack: this.sanitizeStackTrace(error.stack)
        };
        logEntry.context = { ...logEntry.context, ...error.getLoggingDetails() };
      } else {
        logEntry.error = {
          type: error.name || 'Error',
          message: this.sanitizeMessage(error.message),
          stack: this.sanitizeStackTrace(error.stack)
        };
      }
    }

    this.writeLogEntry(logEntry);
    this.writeToErrorLog(logEntry);
  }

  /**
   * Log critical error (system-level issues)
   */
  critical(message: string, error?: Error | AgentError, context?: Record<string, any>): void {
    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level: 'critical',
      message: this.sanitizeMessage(message),
      context: this.sanitizeContext(context)
    };

    if (error) {
      if (error instanceof AgentError) {
        logEntry.error = {
          type: error.type,
          message: error.getSanitizedMessage(),
          severity: error.severity,
          stack: this.sanitizeStackTrace(error.stack)
        };
      } else {
        logEntry.error = {
          type: error.name || 'Error',
          message: this.sanitizeMessage(error.message),
          stack: this.sanitizeStackTrace(error.stack)
        };
      }
    }

    this.writeLogEntry(logEntry);
    this.writeToErrorLog(logEntry);
    
    // Also log to console for immediate attention
    console.error(`[CRITICAL] ${message}`, error);
  }

  /**
   * Log performance metrics
   */
  performance(operation: string, duration: number, context?: Record<string, any>): void {
    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level: 'info',
      message: `Performance: ${operation}`,
      context: this.sanitizeContext(context),
      performance: {
        operation,
        duration
      }
    };

    this.writeLogEntry(logEntry);
  }

  /**
   * Log user action (with privacy protection)
   */
  userAction(action: string, userId: string, username?: string, channelId?: string, guildId?: string, context?: Record<string, any>): void {
    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level: 'info',
      message: `User action: ${action}`,
      context: this.sanitizeContext(context),
      user: {
        id: this.hashUserId(userId),
        username: username ? this.sanitizeUsername(username) : undefined,
        channel: channelId ? this.hashId(channelId) : undefined,
        guild: guildId ? this.hashId(guildId) : undefined
      }
    };

    this.writeLogEntry(logEntry);
  }

  /**
   * Core logging method
   */
  private log(level: LogLevel, message: string, context?: Record<string, any>): void {
    if (!this.shouldLog(level)) {
      return;
    }

    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      message: this.sanitizeMessage(message),
      context: this.sanitizeContext(context)
    };

    this.writeLogEntry(logEntry);
  }

  /**
   * Check if we should log at this level
   */
  private shouldLog(level: LogLevel): boolean {
    const levels: Record<LogLevel, number> = {
      debug: 0,
      info: 1,
      warn: 2,
      error: 3,
      critical: 4
    };

    return levels[level] >= levels[this.currentLogLevel];
  }

  /**
   * Write log entry to file
   */
  private writeLogEntry(entry: LogEntry): void {
    try {
      const logLine = JSON.stringify(entry) + '\n';
      
      // Write to main log file
      fs.appendFileSync(this.logFile, logLine);
      
      // Also log to console in development
      if (process.env.NODE_ENV !== 'production') {
        this.logToConsole(entry);
      }
    } catch (error) {
      // Fallback to console if file writing fails
      console.error('Failed to write to log file:', error);
      this.logToConsole(entry);
    }
  }

  /**
   * Write error entries to separate error log
   */
  private writeToErrorLog(entry: LogEntry): void {
    try {
      const logLine = JSON.stringify(entry) + '\n';
      fs.appendFileSync(this.errorLogFile, logLine);
    } catch (error) {
      console.error('Failed to write to error log file:', error);
    }
  }

  /**
   * Log to console with formatting
   */
  private logToConsole(entry: LogEntry): void {
    const timestamp = new Date(entry.timestamp).toLocaleTimeString();
    const level = entry.level.toUpperCase().padEnd(8);
    
    switch (entry.level) {
      case 'debug':
        console.debug(`${timestamp} [${level}] ${entry.message}`);
        break;
      case 'info':
        console.info(`${timestamp} [${level}] ${entry.message}`);
        break;
      case 'warn':
        console.warn(`${timestamp} [${level}] ${entry.message}`);
        break;
      case 'error':
      case 'critical':
        console.error(`${timestamp} [${level}] ${entry.message}`);
        if (entry.error) {
          console.error('Error details:', entry.error);
        }
        break;
    }
  }

  /**
   * Sanitize message content
   */
  private sanitizeMessage(message: string): string {
    if (!message) return '';
    
    let sanitized = message;
    
    // Remove URLs, IPs, emails, and other sensitive patterns
    sanitized = sanitized.replace(/https?:\/\/[^\s]+/g, '[URL]');
    sanitized = sanitized.replace(/\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/g, '[IP]');
    sanitized = sanitized.replace(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g, '[EMAIL]');
    sanitized = sanitized.replace(/\b\d{15,19}\b/g, '[DISCORD_ID]'); // Discord IDs
    
    return sanitized;
  }

  /**
   * Sanitize context object
   */
  private sanitizeContext(context?: Record<string, any>): Record<string, any> | undefined {
    if (!context) return undefined;

    const sanitized: Record<string, any> = {};
    
    for (const [key, value] of Object.entries(context)) {
      if (this.isSensitiveKey(key)) {
        sanitized[key] = '[REDACTED]';
        continue;
      }

      if (typeof value === 'string') {
        sanitized[key] = this.sanitizeMessage(value);
      } else if (typeof value === 'object' && value !== null) {
        sanitized[key] = this.sanitizeContext(value as Record<string, any>);
      } else {
        sanitized[key] = value;
      }
    }

    return sanitized;
  }

  /**
   * Check if key contains sensitive information
   */
  private isSensitiveKey(key: string): boolean {
    const sensitiveKeys = [
      'token', 'password', 'secret', 'key', 'auth', 'credential',
      'email', 'phone', 'address', 'ssn', 'credit', 'payment',
      'discord_token', 'bot_token'
    ];
    
    return sensitiveKeys.some(sensitive => 
      key.toLowerCase().includes(sensitive)
    );
  }

  /**
   * Sanitize stack trace
   */
  private sanitizeStackTrace(stack?: string): string | undefined {
    if (!stack) return undefined;
    
    // Remove file paths that might contain sensitive information
    return stack.replace(/\/[^\s]+\//g, '/[PATH]/');
  }

  /**
   * Hash user ID for privacy
   */
  private hashUserId(userId: string): string {
    // Simple hash for user ID (in production, use proper hashing)
    return `user_${userId.slice(-4)}`;
  }

  /**
   * Hash generic ID for privacy
   */
  private hashId(id: string): string {
    return `id_${id.slice(-4)}`;
  }

  /**
   * Sanitize username
   */
  private sanitizeUsername(username: string): string {
    // Keep first and last character, replace middle with asterisks
    if (username.length <= 2) return '**';
    return username[0] + '*'.repeat(username.length - 2) + username[username.length - 1];
  }

  /**
   * Rotate log files when they get too large
   */
  private rotateLogs(): void {
    try {
      if (fs.existsSync(this.logFile)) {
        const stats = fs.statSync(this.logFile);
        if (stats.size > this.maxLogFileSize) {
          this.rotateLogFile(this.logFile);
        }
      }

      if (fs.existsSync(this.errorLogFile)) {
        const stats = fs.statSync(this.errorLogFile);
        if (stats.size > this.maxLogFileSize) {
          this.rotateLogFile(this.errorLogFile);
        }
      }
    } catch (error) {
      console.error('Failed to rotate logs:', error);
    }
  }

  /**
   * Rotate a specific log file
   */
  private rotateLogFile(logFile: string): void {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const rotatedFile = `${logFile}.${timestamp}`;
    
    fs.renameSync(logFile, rotatedFile);
    
    // Clean up old rotated files
    this.cleanupOldLogs(path.dirname(logFile), path.basename(logFile));
  }

  /**
   * Clean up old rotated log files
   */
  private cleanupOldLogs(logDir: string, baseFileName: string): void {
    try {
      const files = fs.readdirSync(logDir)
        .filter(file => file.startsWith(baseFileName) && file !== baseFileName)
        .map(file => ({
          name: file,
          path: path.join(logDir, file),
          mtime: fs.statSync(path.join(logDir, file)).mtime
        }))
        .sort((a, b) => b.mtime.getTime() - a.mtime.getTime());

      // Keep only the most recent files
      const filesToDelete = files.slice(this.maxLogFiles);
      
      for (const file of filesToDelete) {
        fs.unlinkSync(file.path);
      }
    } catch (error) {
      console.error('Failed to cleanup old logs:', error);
    }
  }
}

export const logger = new Logger();