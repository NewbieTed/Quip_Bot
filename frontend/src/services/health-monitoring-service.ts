import { getBotConfig } from '../config/bot-config';
import { NetworkErrorHandler } from '../utils/network-error-handler';
import { logger } from '../utils/logger';

/**
 * Health status levels
 */
export type HealthStatus = 'healthy' | 'degraded' | 'unhealthy' | 'critical';

/**
 * Service health information
 */
interface ServiceHealth {
  status: HealthStatus;
  lastCheck: Date;
  responseTime?: number;
  errorRate?: number;
  details?: string;
}

/**
 * Overall system health
 */
interface SystemHealth {
  overall: HealthStatus;
  services: {
    backend: ServiceHealth;
    discord: ServiceHealth;
    network: ServiceHealth;
  };
  lastUpdated: Date;
}

/**
 * Health monitoring service for tracking system status and providing context for errors
 */
export class HealthMonitoringService {
  private static instance: HealthMonitoringService;
  private config = getBotConfig();
  private networkHandler = new NetworkErrorHandler();
  private healthStatus: SystemHealth;
  private monitoringInterval?: NodeJS.Timeout;
  private readonly checkIntervalMs = 60000; // 1 minute

  private constructor() {
    this.healthStatus = {
      overall: 'healthy',
      services: {
        backend: { status: 'healthy', lastCheck: new Date() },
        discord: { status: 'healthy', lastCheck: new Date() },
        network: { status: 'healthy', lastCheck: new Date() }
      },
      lastUpdated: new Date()
    };
  }

  public static getInstance(): HealthMonitoringService {
    if (!HealthMonitoringService.instance) {
      HealthMonitoringService.instance = new HealthMonitoringService();
    }
    return HealthMonitoringService.instance;
  }

  /**
   * Start health monitoring
   */
  public startMonitoring(): void {
    if (this.monitoringInterval) {
      return; // Already monitoring
    }

    logger.info('Starting health monitoring service');
    
    // Initial health check
    this.performHealthCheck();

    // Set up periodic health checks
    this.monitoringInterval = setInterval(() => {
      this.performHealthCheck();
    }, this.checkIntervalMs);
  }

  /**
   * Stop health monitoring
   */
  public stopMonitoring(): void {
    if (this.monitoringInterval) {
      clearInterval(this.monitoringInterval);
      this.monitoringInterval = undefined;
      logger.info('Stopped health monitoring service');
    }
  }

  /**
   * Get current system health status
   */
  public getHealthStatus(): SystemHealth {
    return { ...this.healthStatus };
  }

  /**
   * Get health status for a specific service
   */
  public getServiceHealth(service: keyof SystemHealth['services']): ServiceHealth {
    return { ...this.healthStatus.services[service] };
  }

  /**
   * Check if system is healthy enough for operations
   */
  public isSystemHealthy(): boolean {
    return this.healthStatus.overall === 'healthy' || this.healthStatus.overall === 'degraded';
  }

  /**
   * Get user-friendly health message
   */
  public getHealthMessage(): string {
    const status = this.healthStatus.overall;
    const lastUpdated = this.healthStatus.lastUpdated.toLocaleTimeString();

    switch (status) {
      case 'healthy':
        return `🟢 All systems operational (checked ${lastUpdated})`;
      case 'degraded':
        return `🟡 Some services experiencing issues (checked ${lastUpdated})`;
      case 'unhealthy':
        return `🟠 Multiple services affected (checked ${lastUpdated})`;
      case 'critical':
        return `🔴 System experiencing critical issues (checked ${lastUpdated})`;
      default:
        return `❓ Health status unknown (checked ${lastUpdated})`;
    }
  }

  /**
   * Get detailed health report
   */
  public getDetailedHealthReport(): string {
    const report = [`**System Health Report**`, `Overall Status: ${this.getStatusEmoji(this.healthStatus.overall)} ${this.healthStatus.overall.toUpperCase()}`, ''];

    for (const [serviceName, health] of Object.entries(this.healthStatus.services)) {
      const emoji = this.getStatusEmoji(health.status);
      const responseTime = health.responseTime ? ` (${health.responseTime}ms)` : '';
      const errorRate = health.errorRate ? ` - Error Rate: ${health.errorRate.toFixed(1)}%` : '';
      
      report.push(`${emoji} **${serviceName.toUpperCase()}**: ${health.status}${responseTime}${errorRate}`);
      
      if (health.details) {
        report.push(`   └─ ${health.details}`);
      }
    }

    report.push('', `Last Updated: ${this.healthStatus.lastUpdated.toLocaleString()}`);
    
    return report.join('\n');
  }

  /**
   * Record service error for health tracking
   */
  public recordServiceError(service: keyof SystemHealth['services'], error: Error): void {
    const serviceHealth = this.healthStatus.services[service];
    
    // Update error rate (simple moving average)
    const currentErrorRate = serviceHealth.errorRate || 0;
    serviceHealth.errorRate = Math.min((currentErrorRate + 10) / 2, 100); // Increase error rate
    
    // Update status based on error rate
    if (serviceHealth.errorRate > 50) {
      serviceHealth.status = 'critical';
      serviceHealth.details = 'High error rate detected';
    } else if (serviceHealth.errorRate > 25) {
      serviceHealth.status = 'unhealthy';
      serviceHealth.details = 'Elevated error rate';
    } else if (serviceHealth.errorRate > 10) {
      serviceHealth.status = 'degraded';
      serviceHealth.details = 'Some errors detected';
    }

    serviceHealth.lastCheck = new Date();
    this.updateOverallHealth();

    logger.warn(`Service error recorded for ${service}`, {
      service,
      errorRate: serviceHealth.errorRate,
      newStatus: serviceHealth.status,
      error: error.message
    });
  }

  /**
   * Record successful service operation
   */
  public recordServiceSuccess(service: keyof SystemHealth['services'], responseTime?: number): void {
    const serviceHealth = this.healthStatus.services[service];
    
    // Update response time (simple moving average)
    if (responseTime !== undefined) {
      const currentResponseTime = serviceHealth.responseTime || responseTime;
      serviceHealth.responseTime = Math.round((currentResponseTime + responseTime) / 2);
    }

    // Improve error rate
    const currentErrorRate = serviceHealth.errorRate || 0;
    serviceHealth.errorRate = Math.max(currentErrorRate * 0.9, 0); // Decay error rate

    // Update status based on improved metrics
    if (serviceHealth.errorRate < 5) {
      serviceHealth.status = 'healthy';
      serviceHealth.details = undefined;
    } else if (serviceHealth.errorRate < 15) {
      serviceHealth.status = 'degraded';
      serviceHealth.details = 'Minor issues detected';
    }

    serviceHealth.lastCheck = new Date();
    this.updateOverallHealth();
  }

  /**
   * Perform comprehensive health check
   */
  private async performHealthCheck(): Promise<void> {
    logger.debug('Performing health check');

    try {
      // Check backend health
      await this.checkBackendHealth();
      
      // Check network health
      await this.checkNetworkHealth();
      
      // Discord health is checked passively through operation success/failure
      
      this.updateOverallHealth();
      this.healthStatus.lastUpdated = new Date();

      logger.debug('Health check completed', {
        overall: this.healthStatus.overall,
        backend: this.healthStatus.services.backend.status,
        network: this.healthStatus.services.network.status
      });

    } catch (error) {
      logger.error('Health check failed', error as Error);
      this.healthStatus.services.network.status = 'unhealthy';
      this.healthStatus.services.network.details = 'Health check failed';
      this.updateOverallHealth();
    }
  }

  /**
   * Check backend service health
   */
  private async checkBackendHealth(): Promise<void> {
    const startTime = Date.now();
    
    try {
      // Simple health check to backend
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      
      try {
        const response = await fetch(`${this.config.backendApiUrl}/health`, {
          method: 'GET',
          signal: controller.signal
        });
        
        clearTimeout(timeoutId);

        const responseTime = Date.now() - startTime;

        if (response.ok) {
          this.recordServiceSuccess('backend', responseTime);
        } else {
          throw new Error(`Backend health check failed: ${response.status}`);
        }
      } catch (error) {
        clearTimeout(timeoutId);
        throw error;
      }

    } catch (error) {
      this.recordServiceError('backend', error as Error);
    }
  }

  /**
   * Check network health using circuit breaker status
   */
  private async checkNetworkHealth(): Promise<void> {
    const circuitStatus = this.networkHandler.getCircuitBreakerStatus();
    const operationMetrics = this.networkHandler.getOperationMetrics();
    
    // Calculate overall success rate
    const totalOperations = Object.values(operationMetrics).reduce((sum, metrics) => sum + metrics.attempts, 0);
    const totalFailures = Object.values(operationMetrics).reduce((sum, metrics) => sum + metrics.failures, 0);
    const successRate = totalOperations > 0 ? ((totalOperations - totalFailures) / totalOperations) * 100 : 100;

    const networkHealth = this.healthStatus.services.network;
    networkHealth.errorRate = 100 - successRate;
    networkHealth.lastCheck = new Date();

    if (circuitStatus.state === 'open') {
      networkHealth.status = 'critical';
      networkHealth.details = 'Circuit breaker is open';
    } else if (successRate < 50) {
      networkHealth.status = 'unhealthy';
      networkHealth.details = `Low success rate: ${successRate.toFixed(1)}%`;
    } else if (successRate < 80) {
      networkHealth.status = 'degraded';
      networkHealth.details = `Reduced success rate: ${successRate.toFixed(1)}%`;
    } else {
      networkHealth.status = 'healthy';
      networkHealth.details = undefined;
    }
  }

  /**
   * Update overall system health based on service health
   */
  private updateOverallHealth(): void {
    const services = Object.values(this.healthStatus.services);
    const criticalCount = services.filter(s => s.status === 'critical').length;
    const unhealthyCount = services.filter(s => s.status === 'unhealthy').length;
    const degradedCount = services.filter(s => s.status === 'degraded').length;

    if (criticalCount > 0) {
      this.healthStatus.overall = 'critical';
    } else if (unhealthyCount > 1) {
      this.healthStatus.overall = 'unhealthy';
    } else if (unhealthyCount > 0 || degradedCount > 1) {
      this.healthStatus.overall = 'degraded';
    } else {
      this.healthStatus.overall = 'healthy';
    }
  }

  /**
   * Get emoji for health status
   */
  private getStatusEmoji(status: HealthStatus): string {
    switch (status) {
      case 'healthy': return '🟢';
      case 'degraded': return '🟡';
      case 'unhealthy': return '🟠';
      case 'critical': return '🔴';
      default: return '❓';
    }
  }
}

// Export singleton instance
export const healthMonitoringService = HealthMonitoringService.getInstance();