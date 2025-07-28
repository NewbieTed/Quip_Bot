# Redis Cache Configuration Usage

This document describes how to use the Redis cache configuration implemented in task 5.1.

## Overview

The Redis cache integration provides:
- **CacheManager bean configuration** with cache-specific TTL settings
- **Custom cache key generators** for consistent key generation
- **Cache-specific TTL settings** for different types of data

## Cache Names and TTL Settings

The following cache names are pre-configured with specific TTL values:

| Cache Name | TTL | Use Case |
|------------|-----|----------|
| `toolWhitelist` | 1 hour | Tool whitelist data |
| `problemCategories` | 24 hours | Problem categories (static data) |
| `serverData` | 2 hours | Server information |
| `memberData` | 30 minutes | Member information |
| `assistantSession` | 15 minutes | Assistant session data |
| `temporaryData` | 5 minutes | Temporary calculations |

## Usage Examples

### Using @Cacheable Annotation

```java
@Service
public class ToolWhitelistService {
    
    @Cacheable(value = "toolWhitelist", key = "#serverId")
    public List<ToolWhitelist> getToolWhitelist(String serverId) {
        // This method result will be cached for 1 hour
        return toolWhitelistMapper.findByServerId(serverId);
    }
    
    @CacheEvict(value = "toolWhitelist", key = "#serverId")
    public void updateToolWhitelist(String serverId, List<ToolWhitelist> whitelist) {
        // This will evict the cache entry for the specific server
        toolWhitelistMapper.updateByServerId(serverId, whitelist);
    }
}
```

### Using Custom Cache Key Generator

```java
@Service
public class ProblemService {
    
    @Cacheable(value = "problemCategories", keyGenerator = "customCacheKeyGenerator")
    public List<ProblemCategory> getAllCategories() {
        // Uses custom key generator: "ProblemService:getAllCategories"
        return problemCategoryMapper.findAll();
    }
    
    @Cacheable(value = "problemCategories", keyGenerator = "customCacheKeyGenerator")
    public ProblemCategory getCategoryById(Long categoryId) {
        // Uses custom key generator: "ProblemService:getCategoryById:123"
        return problemCategoryMapper.findById(categoryId);
    }
}
```

### Using Cache with Conditions

```java
@Service
public class MemberService {
    
    @Cacheable(value = "memberData", key = "#memberId", condition = "#memberId != null")
    public Member getMemberById(String memberId) {
        // Only cache if memberId is not null
        return memberMapper.findById(memberId);
    }
    
    @CachePut(value = "memberData", key = "#member.id")
    public Member updateMember(Member member) {
        // Always update the cache with the new value
        memberMapper.update(member);
        return member;
    }
}
```

## Configuration

### Application Properties

The cache configuration is controlled by these properties in `application.yml`:

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # Default TTL (1 hour)
      cache-null-values: false
      key-prefix: "quip:backend:"
    cache-names:
      - toolWhitelist
      - problemCategories
      - serverData
      - memberData
      - assistantSession
      - temporaryData
```

### Custom Cache Key Generator

The `CustomCacheKeyGenerator` creates keys in the format:
```
ClassName:methodName:param1:param2:...
```

Examples:
- `ToolWhitelistService:getToolWhitelist:server123`
- `ProblemService:getAllCategories`
- `MemberService:getMemberById:member456`

## Cache-Specific Configurations

Each cache has its own TTL configuration:

```java
// Tool whitelist cache - 1 hour TTL
@Cacheable(value = "toolWhitelist")

// Problem categories cache - 24 hours TTL (static data)
@Cacheable(value = "problemCategories")

// Server data cache - 2 hours TTL
@Cacheable(value = "serverData")

// Member data cache - 30 minutes TTL
@Cacheable(value = "memberData")

// Assistant session cache - 15 minutes TTL
@Cacheable(value = "assistantSession")

// Temporary data cache - 5 minutes TTL
@Cacheable(value = "temporaryData")
```

## Best Practices

1. **Use appropriate cache names** based on data type and expected TTL
2. **Use meaningful cache keys** that uniquely identify the cached data
3. **Implement cache eviction** when data is updated
4. **Use conditions** to avoid caching null or invalid data
5. **Monitor cache hit ratios** to ensure effectiveness

## Testing

The cache configuration includes comprehensive unit tests:
- `CustomCacheKeyGeneratorTest` - Tests key generation logic
- `CacheConfigurationTest` - Tests cache manager configuration

Run tests with:
```bash
./gradlew test --tests "com.quip.backend.config.CustomCacheKeyGeneratorTest"
./gradlew test --tests "com.quip.backend.config.CacheConfigurationTest"
```