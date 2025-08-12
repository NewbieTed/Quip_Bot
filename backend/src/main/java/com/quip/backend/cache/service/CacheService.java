package com.quip.backend.cache.service;

import com.quip.backend.cache.util.CacheKeyManager;
import com.quip.backend.member.model.Member;
import com.quip.backend.problem.model.ProblemCategory;
import com.quip.backend.redis.service.RedisService;
import com.quip.backend.server.model.Server;
import com.quip.backend.tool.model.ToolWhitelist;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Service providing application-specific caching patterns for the Quip backend.
 * <p>
 * This service provides high-level caching operations for:
 * - Tool whitelist data caching and invalidation
 * - Problem categories caching functionality
 * - Server and member data caching methods
 * - Generic caching with application-specific key patterns
 * </p>
 * <p>
 * All cache keys follow the pattern: {keyPrefix}:{dataType}:{identifier}
 * TTL values are configurable and default to application settings.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisService redisService;
    private final CacheKeyManager cacheKeyManager;

    @Getter
    @Value("${app.redis.default-ttl:3600}")
    private long defaultTtlSeconds;

    // ========== Tool Whitelist Caching ==========

    /**
     * Caches tool whitelist data for a specific server.
     *
     * @param serverId  the server ID
     * @param whitelist the list of tool whitelist entries to cache
     * @throws IllegalArgumentException if serverId is null or whitelist is null
     */
    public void cacheToolWhitelist(Long serverId, List<ToolWhitelist> whitelist) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (whitelist == null) {
            throw new IllegalArgumentException("Whitelist cannot be null");
        }

        String key = cacheKeyManager.getToolWhitelistKey(serverId);
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds);
        
        redisService.set(key, whitelist, ttl);
        log.debug("Cached tool whitelist for server: {} with {} entries", serverId, whitelist.size());
    }

    /**
     * Retrieves cached tool whitelist data for a specific server.
     *
     * @param serverId the server ID
     * @return the cached tool whitelist entries, or null if not found
     * @throws IllegalArgumentException if serverId is null
     */
    @SuppressWarnings("unchecked")
    public List<ToolWhitelist> getToolWhitelist(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getToolWhitelistKey(serverId);
        List<ToolWhitelist> result = redisService.get(key, List.class);
        
        if (result != null) {
            log.debug("Retrieved cached tool whitelist for server: {} with {} entries", serverId, result.size());
        } else {
            log.debug("No cached tool whitelist found for server: {}", serverId);
        }
        
        return result;
    }

    /**
     * Evicts cached tool whitelist data for a specific server.
     *
     * @param serverId the server ID
     * @return true if the cache entry was evicted, false if it didn't exist
     * @throws IllegalArgumentException if serverId is null
     */
    public boolean evictToolWhitelist(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getToolWhitelistKey(serverId);
        boolean evicted = redisService.delete(key);
        
        log.debug("Evicted tool whitelist cache for server: {} - result: {}", serverId, evicted);
        return evicted;
    }

    /**
     * Caches tool whitelist data for a specific member in a server.
     *
     * @param serverId  the server ID
     * @param memberId  the member ID
     * @param whitelist the list of tool whitelist entries to cache
     * @throws IllegalArgumentException if serverId, memberId, or whitelist is null
     */
    public void cacheToolWhitelistForMember(Long serverId, Long memberId, List<ToolWhitelist> whitelist) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (whitelist == null) {
            throw new IllegalArgumentException("Whitelist cannot be null");
        }

        String key = cacheKeyManager.getToolWhitelistMemberKey(serverId, memberId);
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds);
        
        redisService.set(key, whitelist, ttl);
        log.debug("Cached tool whitelist for server: {} and member: {} with {} entries", 
                 serverId, memberId, whitelist.size());
    }

    /**
     * Retrieves cached tool whitelist data for a specific member in a server.
     *
     * @param serverId the server ID
     * @param memberId the member ID
     * @return the cached tool whitelist entries, or null if not found
     * @throws IllegalArgumentException if serverId or memberId is null
     */
    @SuppressWarnings("unchecked")
    public List<ToolWhitelist> getToolWhitelistForMember(Long serverId, Long memberId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }

        String key = cacheKeyManager.getToolWhitelistMemberKey(serverId, memberId);
        List<ToolWhitelist> result = redisService.get(key, List.class);
        
        if (result != null) {
            log.debug("Retrieved cached tool whitelist for server: {} and member: {} with {} entries", 
                     serverId, memberId, result.size());
        } else {
            log.debug("No cached tool whitelist found for server: {} and member: {}", serverId, memberId);
        }
        
        return result;
    }

    /**
     * Evicts cached tool whitelist data for a specific member in a server.
     *
     * @param serverId the server ID
     * @param memberId the member ID
     * @return true if the cache entry was evicted, false if it didn't exist
     * @throws IllegalArgumentException if serverId or memberId is null
     */
    public boolean evictToolWhitelistForMember(Long serverId, Long memberId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }

        String key = cacheKeyManager.getToolWhitelistMemberKey(serverId, memberId);
        boolean evicted = redisService.delete(key);
        
        log.debug("Evicted tool whitelist cache for server: {} and member: {} - result: {}", 
                 serverId, memberId, evicted);
        return evicted;
    }

    // ========== Problem Categories Caching ==========

    /**
     * Caches problem categories for a specific server.
     *
     * @param serverId   the server ID
     * @param categories the list of problem categories to cache
     * @throws IllegalArgumentException if serverId or categories is null
     */
    public void cacheProblemCategories(Long serverId, List<ProblemCategory> categories) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (categories == null) {
            throw new IllegalArgumentException("Categories cannot be null");
        }

        String key = cacheKeyManager.getProblemCategoriesKey(serverId);
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds * 24); // 24 hours for categories (more static)
        
        redisService.set(key, categories, ttl);
        log.debug("Cached problem categories for server: {} with {} entries", serverId, categories.size());
    }

    /**
     * Retrieves cached problem categories for a specific server.
     *
     * @param serverId the server ID
     * @return the cached problem categories, or null if not found
     * @throws IllegalArgumentException if serverId is null
     */
    @SuppressWarnings("unchecked")
    public List<ProblemCategory> getProblemCategories(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getProblemCategoriesKey(serverId);
        List<ProblemCategory> result = redisService.get(key, List.class);
        
        if (result != null) {
            log.debug("Retrieved cached problem categories for server: {} with {} entries", serverId, result.size());
        } else {
            log.debug("No cached problem categories found for server: {}", serverId);
        }
        
        return result;
    }

    /**
     * Evicts cached problem categories for a specific server.
     *
     * @param serverId the server ID
     * @return true if the cache entry was evicted, false if it didn't exist
     * @throws IllegalArgumentException if serverId is null
     */
    public boolean evictProblemCategories(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getProblemCategoriesKey(serverId);
        boolean evicted = redisService.delete(key);
        
        log.debug("Evicted problem categories cache for server: {} - result: {}", serverId, evicted);
        return evicted;
    }

    // ========== Server Data Caching ==========

    /**
     * Caches server data.
     *
     * @param server the server to cache
     * @throws IllegalArgumentException if server or server ID is null
     */
    public void cacheServer(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }
        if (server.getId() == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getServerKey(server.getId());
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds * 6); // 6 hours for server data
        
        redisService.set(key, server, ttl);
        log.debug("Cached server data for server: {} ({})", server.getId(), server.getServerName());
    }

    /**
     * Retrieves cached server data.
     *
     * @param serverId the server ID
     * @return the cached server, or null if not found
     * @throws IllegalArgumentException if serverId is null
     */
    public Server getServer(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getServerKey(serverId);
        Server result = redisService.get(key, Server.class);
        
        if (result != null) {
            log.debug("Retrieved cached server data for server: {} ({})", serverId, result.getServerName());
        } else {
            log.debug("No cached server data found for server: {}", serverId);
        }
        
        return result;
    }

    /**
     * Evicts cached server data.
     *
     * @param serverId the server ID
     * @return true if the cache entry was evicted, false if it didn't exist
     * @throws IllegalArgumentException if serverId is null
     */
    public boolean evictServer(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String key = cacheKeyManager.getServerKey(serverId);
        boolean evicted = redisService.delete(key);
        
        log.debug("Evicted server cache for server: {} - result: {}", serverId, evicted);
        return evicted;
    }

    // ========== Member Data Caching ==========

    /**
     * Caches member data.
     *
     * @param member the member to cache
     * @throws IllegalArgumentException if member or member ID is null
     */
    public void cacheMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member cannot be null");
        }
        if (member.getId() == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }

        String key = cacheKeyManager.getMemberKey(member.getId());
        Duration ttl = Duration.ofSeconds(defaultTtlSeconds * 2); // 2 hours for member data
        
        redisService.set(key, member, ttl);
        log.debug("Cached member data for member: {} ({})", member.getId(), member.getMemberName());
    }

    /**
     * Retrieves cached member data.
     *
     * @param memberId the member ID
     * @return the cached member, or null if not found
     * @throws IllegalArgumentException if memberId is null
     */
    public Member getMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }

        String key = cacheKeyManager.getMemberKey(memberId);
        Member result = redisService.get(key, Member.class);
        
        if (result != null) {
            log.debug("Retrieved cached member data for member: {} ({})", memberId, result.getMemberName());
        } else {
            log.debug("No cached member data found for member: {}", memberId);
        }
        
        return result;
    }

    /**
     * Evicts cached member data.
     *
     * @param memberId the member ID
     * @return true if the cache entry was evicted, false if it didn't exist
     * @throws IllegalArgumentException if memberId is null
     */
    public boolean evictMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }

        String key = cacheKeyManager.getMemberKey(memberId);
        boolean evicted = redisService.delete(key);
        
        log.debug("Evicted member cache for member: {} - result: {}", memberId, evicted);
        return evicted;
    }

    // ========== Generic Caching Methods ==========

    /**
     * Caches data with a custom TTL using application-specific key patterns.
     *
     * @param keyPrefix  the key prefix for the data type
     * @param identifier the unique identifier for the data
     * @param data       the data to cache
     * @param ttl        the time to live duration
     * @throws IllegalArgumentException if any parameter is null or keyPrefix/identifier is empty
     */
    public void cacheWithTTL(String keyPrefix, String identifier, Object data, Duration ttl) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Key prefix cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (ttl == null) {
            throw new IllegalArgumentException("TTL cannot be null");
        }

        String key = cacheKeyManager.getCustomKey(keyPrefix, identifier);
        redisService.set(key, data, ttl);
        log.debug("Cached data with key prefix: {} and identifier: {} with TTL: {}", keyPrefix, identifier, ttl);
    }

    /**
     * Retrieves cached data using application-specific key patterns.
     *
     * @param keyPrefix  the key prefix for the data type
     * @param identifier the unique identifier for the data
     * @param type       the expected type of the cached data
     * @param <T>        the type parameter
     * @return the cached data cast to the specified type, or null if not found
     * @throws IllegalArgumentException if keyPrefix/identifier is null/empty or type is null
     */
    public <T> T getFromCache(String keyPrefix, String identifier, Class<T> type) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Key prefix cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        String key = cacheKeyManager.getCustomKey(keyPrefix, identifier);
        T result = redisService.get(key, type);
        
        if (result != null) {
            log.debug("Retrieved cached data with key prefix: {} and identifier: {}", keyPrefix, identifier);
        } else {
            log.debug("No cached data found with key prefix: {} and identifier: {}", keyPrefix, identifier);
        }
        
        return result;
    }

    /**
     * Evicts cached data using application-specific key patterns.
     *
     * @param keyPrefix  the key prefix for the data type
     * @param identifier the unique identifier for the data
     * @return true if the cache entry was evicted, false if it didn't exist
     * @throws IllegalArgumentException if keyPrefix or identifier is null or empty
     */
    public boolean evictFromCache(String keyPrefix, String identifier) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Key prefix cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        String key = cacheKeyManager.getCustomKey(keyPrefix, identifier);
        boolean evicted = redisService.delete(key);
        
        log.debug("Evicted cache with key prefix: {} and identifier: {} - result: {}", 
                 keyPrefix, identifier, evicted);
        return evicted;
    }

    /**
     * Evicts all cached data matching a specific key pattern.
     *
     * @param keyPrefix the key prefix pattern to match
     * @return the number of keys evicted
     * @throws IllegalArgumentException if keyPrefix is null or empty
     */
    public long evictByPattern(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Key prefix cannot be null or empty");
        }

        String pattern = cacheKeyManager.getCustomPattern(keyPrefix);
        long evicted = redisService.deleteByPattern(pattern);
        
        log.debug("Evicted {} cache entries matching pattern: {}", evicted, pattern);
        return evicted;
    }

    /**
     * Checks if cached data exists using application-specific key patterns.
     *
     * @param keyPrefix  the key prefix for the data type
     * @param identifier the unique identifier for the data
     * @return true if the cache entry exists, false otherwise
     * @throws IllegalArgumentException if keyPrefix or identifier is null or empty
     */
    public boolean existsInCache(String keyPrefix, String identifier) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Key prefix cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        String key = cacheKeyManager.getCustomKey(keyPrefix, identifier);
        boolean exists = redisService.exists(key);
        
        log.debug("Cache existence check with key prefix: {} and identifier: {} - result: {}", 
                 keyPrefix, identifier, exists);
        return exists;
    }

    // ========== Utility Methods ==========

    /**
     * Gets the configured key prefix from the cache key manager.
     *
     * @return the application prefix
     */
    public String getKeyPrefix() {
        return cacheKeyManager.getApplicationPrefix();
    }

    /**
     * Gets the current cache version from the cache key manager.
     *
     * @return the current cache version
     */
    public String getCurrentCacheVersion() {
        return cacheKeyManager.getCurrentVersion();
    }

    /**
     * Evicts all cached data matching a tool whitelist pattern for a server.
     *
     * @param serverId the server ID
     * @return the number of keys evicted
     * @throws IllegalArgumentException if serverId is null
     */
    public long evictAllToolWhitelistForServer(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String pattern = cacheKeyManager.getToolWhitelistPattern(serverId);
        long evicted = redisService.deleteByPattern(pattern);
        
        log.debug("Evicted {} tool whitelist cache entries for server: {}", evicted, serverId);
        return evicted;
    }

    /**
     * Evicts all cached data matching a server pattern.
     *
     * @param serverId the server ID
     * @return the number of keys evicted
     * @throws IllegalArgumentException if serverId is null
     */
    public long evictAllServerData(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String pattern = cacheKeyManager.getServerPattern(serverId);
        long evicted = redisService.deleteByPattern(pattern);
        
        log.debug("Evicted {} server cache entries for server: {}", evicted, serverId);
        return evicted;
    }

    /**
     * Evicts all cached data matching a member pattern for a server.
     *
     * @param serverId the server ID
     * @return the number of keys evicted
     * @throws IllegalArgumentException if serverId is null
     */
    public long evictAllMemberDataForServer(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }

        String pattern = cacheKeyManager.getMemberPattern(serverId);
        long evicted = redisService.deleteByPattern(pattern);
        
        log.debug("Evicted {} member cache entries for server: {}", evicted, serverId);
        return evicted;
    }
}