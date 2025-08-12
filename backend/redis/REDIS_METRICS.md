# Redis Metrics and Monitoring

This document describes the Redis metrics and monitoring capabilities implemented in the application.

## Overview

The Redis metrics system provides comprehensive monitoring of Redis operations including:
- Cache hit/miss ratio tracking
- Response time metrics for Redis operations
- Connection pool monitoring
- Error tracking

## Metrics Available

### Cache Metrics

- **redis.cache.hits** (Counter): Number of cache hits
- **redis.cache.misses** (Counter): Number of cache misses
- **redis.cache.errors** (Counter): Number of cache operation errors
- **redis.cache.hit.ratio** (Gauge): Cache hit ratio (0.0 to 1.0)

### Operation Metrics

- **redis.operations.get** (Counter): Number of GET operations
- **redis.operations.set** (Counter): Number of SET operations
- **redis.operations.delete** (Counter): Number of DELETE operations
- **redis.operations.exists** (Counter): Number of EXISTS operations

### Response Time Metrics

- **redis.operations.get.duration** (Timer): Duration of GET operations
- **redis.operations.set.duration** (Timer): Duration of SET operations
- **redis.operations.delete.duration** (Timer): Duration of DELETE operations
- **redis.operations.exists.duration** (Timer): Duration of EXISTS operations

### Connection Pool Metrics

- **redis.connections.active** (Gauge): Number of active Redis connections
- **redis.connections.idle** (Gauge): Number of idle Redis connections
- **redis.connections.total** (Gauge): Total number of Redis connections

## How It Works

### Automatic Metrics Collection

The `RedisMetricsService` is automatically integrated with the `RedisService` and collects metrics for all Redis operations:

1. **Operation Timing**: Each Redis operation is timed and recorded
2. **Cache Hit/Miss Tracking**: GET operations automatically track hits and misses
3. **Error Tracking**: Failed operations are recorded as errors
4. **Connection Monitoring**: Connection pool status is updated periodically

### Integration Points

- **RedisService**: All Redis operations automatically record metrics
- **RedisHealthIndicator**: Health checks include application-level metrics
- **Spring Boot Actuator**: Metrics are exposed through `/actuator/metrics` endpoints

## Accessing Metrics

### Via Actuator Endpoints

Metrics are available through Spring Boot Actuator endpoints:

```bash
# Get all Redis metrics
curl http://localhost:8080/actuator/metrics | grep redis

# Get specific metric
curl http://localhost:8080/actuator/metrics/redis.cache.hits
curl http://localhost:8080/actuator/metrics/redis.operations.get.duration
curl http://localhost:8080/actuator/metrics/redis.cache.hit.ratio
```

### Via Health Endpoint

Application-level metrics are included in the Redis health check:

```bash
curl http://localhost:8080/actuator/health/redis
```

Response includes:
```json
{
  "status": "UP",
  "applicationMetrics": {
    "cache": {
      "hitRatio": "75.00%",
      "totalOperations": 100,
      "totalErrors": 5
    },
    "responseTimes": {
      "averageGetTime": "15.50ms",
      "averageSetTime": "12.30ms",
      "averageDeleteTime": "8.70ms",
      "averageExistsTime": "6.20ms"
    }
  }
}
```

## Configuration

### Enabling Metrics

Metrics are automatically enabled when Redis is enabled. No additional configuration is required.

### Scheduled Updates

Connection pool metrics are updated every 30 seconds via a scheduled task in `RedisConfig`.

## Monitoring and Alerting

### Key Metrics to Monitor

1. **Cache Hit Ratio**: Should be > 70% for good performance
2. **Response Times**: Should be < 50ms for most operations
3. **Error Rate**: Should be < 1% of total operations
4. **Connection Pool**: Active connections should not exceed pool limits

### Sample Alerts

- Alert if cache hit ratio drops below 50%
- Alert if average response time exceeds 100ms
- Alert if error rate exceeds 5%
- Alert if connection pool is exhausted

## Development

### Adding New Metrics

To add new metrics:

1. Add the metric to `RedisMetricsService`
2. Update the service to record the metric at appropriate points
3. Add tests for the new metric
4. Update this documentation

### Testing

Metrics functionality is tested at multiple levels:

- **Unit Tests**: `RedisMetricsServiceTest`
- **Integration Tests**: `RedisMetricsIntegrationTest`
- **Health Indicator Tests**: `RedisHealthIndicatorTest`

Run tests with:
```bash
./gradlew test --tests "*redis*"
```

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing**: Ensure Redis is enabled and operations are being performed
2. **Connection Metrics at Zero**: Check Redis connection configuration
3. **High Error Rates**: Check Redis server status and network connectivity

### Debug Information

Enable debug logging for metrics:
```properties
logging.level.com.quip.backend.redis.metrics=DEBUG
```

This will log detailed information about metric recording and connection pool updates.