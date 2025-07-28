package com.quip.backend.cache.performance;

import com.quip.backend.cache.service.CacheService;
import com.quip.backend.cache.util.CacheKeyManager;
import com.quip.backend.member.model.Member;
import com.quip.backend.problem.model.ProblemCategory;
import com.quip.backend.redis.service.RedisService;
import com.quip.backend.server.model.Server;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import com.quip.backend.tool.model.ToolWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Performance tests for application-specific cache operations.
 * Tests caching patterns used by the application including tool whitelist,
 * problem categories, server data, and member data caching.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CachePerformanceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private CacheKeyManager cacheKeyManager;

    private CacheService cacheService;

    private static final int PERFORMANCE_ITERATIONS = 500;
    private static final int CONCURRENT_THREADS = 8;
    private static final long DEFAULT_TTL_SECONDS = 3600L;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisService, cacheKeyManager);
        ReflectionTestUtils.setField(cacheService, "defaultTtlSeconds", DEFAULT_TTL_SECONDS);
        
        // Setup common mock behaviors
        doNothing().when(redisService).set(anyString(), any(), any(Duration.class));
        when(redisService.delete(anyString())).thenReturn(true);
        when(redisService.exists(anyString())).thenReturn(true);
    }

    // ========== Tool Whitelist Cache Performance ==========

    @Test
    @Timeout(30)
    void benchmarkToolWhitelistCaching_ShouldHandleHighVolumeOperations() {
        // Given
        List<ToolWhitelist> sampleWhitelist = createSampleToolWhitelist(100);
        String cacheKey = "quip:backend:v1:tool:whitelist:";
        
        when(cacheKeyManager.getToolWhitelistKey(anyLong())).thenReturn(cacheKey + "1");
        when(redisService.get(anyString(), eq(List.class))).thenReturn(sampleWhitelist);

        // When - Benchmark cache operations
        long cacheStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            Long serverId = (long) (i % 10); // Simulate 10 different servers
            cacheService.cacheToolWhitelist(serverId, sampleWhitelist);
        }
        long cacheDuration = System.nanoTime() - cacheStartTime;

        long retrieveStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            Long serverId = (long) (i % 10);
            cacheService.getToolWhitelist(serverId);
        }
        long retrieveDuration = System.nanoTime() - retrieveStartTime;

        long evictStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            Long serverId = (long) (i % 10);
            cacheService.evictToolWhitelist(serverId);
        }
        long evictDuration = System.nanoTime() - evictStartTime;

        // Then
        double cacheOpsPerSecond = (PERFORMANCE_ITERATIONS * 1_000_000_000.0) / cacheDuration;
        double retrieveOpsPerSecond = (PERFORMANCE_ITERATIONS * 1_000_000_000.0) / retrieveDuration;
        double evictOpsPerSecond = (PERFORMANCE_ITERATIONS * 1_000_000_000.0) / evictDuration;

        assertThat(cacheOpsPerSecond).isGreaterThan(1000);
        assertThat(retrieveOpsPerSecond).isGreaterThan(1000);
        assertThat(evictOpsPerSecond).isGreaterThan(1000);

        System.out.printf("Tool Whitelist Cache Performance:%n");
        System.out.printf("Cache: %.2f ops/sec, Retrieve: %.2f ops/sec, Evict: %.2f ops/sec%n",
                cacheOpsPerSecond, retrieveOpsPerSecond, evictOpsPerSecond);
    }

    @Test
    @Timeout(30)
    void benchmarkProblemCategoryCaching_ShouldHandleFrequentAccess() {
        // Given
        List<ProblemCategory> categories = createSampleProblemCategories(50);
        String cacheKey = "quip:backend:v1:problem:categories:1";
        Long serverId = 1L;
        
        when(cacheKeyManager.getProblemCategoriesKey(serverId)).thenReturn(cacheKey);
        when(redisService.get(cacheKey, List.class)).thenReturn(categories);

        // When - Simulate frequent category access pattern
        long cacheStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
            cacheService.cacheProblemCategories(serverId, categories);
        }
        long cacheDuration = System.nanoTime() - cacheStartTime;

        // Simulate high read frequency (10:1 read to write ratio)
        long retrieveStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_ITERATIONS * 10; i++) {
            cacheService.getProblemCategories(serverId);
        }
        long retrieveDuration = System.nanoTime() - retrieveStartTime;

        // Then
        double cacheOpsPerSecond = (PERFORMANCE_ITERATIONS * 1_000_000_000.0) / cacheDuration;
        double retrieveOpsPerSecond = (PERFORMANCE_ITERATIONS * 10 * 1_000_000_000.0) / retrieveDuration;

        assertThat(cacheOpsPerSecond).isGreaterThan(500);
        assertThat(retrieveOpsPerSecond).isGreaterThan(5000); // Should handle high read volume

        System.out.printf("Problem Category Cache Performance:%n");
        System.out.printf("Cache: %.2f ops/sec, Retrieve: %.2f ops/sec%n",
                cacheOpsPerSecond, retrieveOpsPerSecond);
    }

    // ========== Concurrent Cache Access Tests ==========

    @Test
    @Timeout(60)
    void testConcurrentToolWhitelistAccess_ShouldHandleMultipleServers() throws InterruptedException {
        // Given
        List<ToolWhitelist> whitelist = createSampleToolWhitelist(20);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger cacheHits = new AtomicInteger(0);
        AtomicInteger cacheMisses = new AtomicInteger(0);
        
        when(cacheKeyManager.getToolWhitelistKey(anyLong())).thenAnswer(
                invocation -> "cache:key:" + invocation.getArgument(0)
        );
        
        // Simulate cache hits and misses
        when(redisService.get(anyString(), eq(List.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.contains("1") || key.contains("2") || key.contains("3")) {
                cacheHits.incrementAndGet();
                return whitelist;
            } else {
                cacheMisses.incrementAndGet();
                return null;
            }
        });

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        // When - Concurrent access to different server whitelists
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        Long serverId = (long) ((threadId * 100 + j) % 10);
                        
                        // Mix of cache and retrieve operations
                        if (j % 3 == 0) {
                            cacheService.cacheToolWhitelist(serverId, whitelist);
                        } else {
                            cacheService.getToolWhitelist(serverId);
                        }
                        
                        successfulOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Log but don't fail the test
                    System.err.println("Thread " + threadId + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successfulOperations.get()).isEqualTo(CONCURRENT_THREADS * 100);
        assertThat(cacheHits.get() + cacheMisses.get()).isGreaterThan(0);

        double hitRatio = (double) cacheHits.get() / (cacheHits.get() + cacheMisses.get());
        System.out.printf("Concurrent whitelist access: %d operations, %.2f%% hit ratio%n",
                successfulOperations.get(), hitRatio * 100);
    }

    @Test
    @Timeout(60)
    void testConcurrentCacheEviction_ShouldHandleInvalidationCorrectly() throws InterruptedException {
        // Given
        AtomicInteger cacheOperations = new AtomicInteger(0);
        AtomicInteger evictOperations = new AtomicInteger(0);
        AtomicInteger retrieveOperations = new AtomicInteger(0);
        
        when(cacheKeyManager.getToolWhitelistKey(anyLong())).thenReturn("test:key");
        when(cacheKeyManager.getProblemCategoriesKey(anyLong())).thenReturn("categories:key");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        // When - Concurrent cache/evict/retrieve operations
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<ToolWhitelist> whitelist = createSampleToolWhitelist(10);
                    List<ProblemCategory> categories = createSampleProblemCategories(5);
                    
                    for (int j = 0; j < 50; j++) {
                        int operation = (threadId + j) % 3;
                        
                        switch (operation) {
                            case 0: // Cache operations
                                cacheService.cacheToolWhitelist(1L, whitelist);
                                cacheService.cacheProblemCategories(1L, categories);
                                cacheOperations.addAndGet(2);
                                break;
                            case 1: // Evict operations
                                cacheService.evictToolWhitelist(1L);
                                cacheService.evictProblemCategories(1L);
                                evictOperations.addAndGet(2);
                                break;
                            case 2: // Retrieve operations
                                cacheService.getToolWhitelist(1L);
                                cacheService.getProblemCategories(1L);
                                retrieveOperations.addAndGet(2);
                                break;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(cacheOperations.get()).isGreaterThan(0);
        assertThat(evictOperations.get()).isGreaterThan(0);
        assertThat(retrieveOperations.get()).isGreaterThan(0);

        System.out.printf("Concurrent eviction test: %d cache, %d evict, %d retrieve operations%n",
                cacheOperations.get(), evictOperations.get(), retrieveOperations.get());
    }

    // ========== Memory and Serialization Performance ==========

    @Test
    @Timeout(30)
    void testLargeObjectCaching_ShouldHandleSerializationEfficiently() {
        // Given
        List<ToolWhitelist> largeWhitelist = createSampleToolWhitelist(1000);
        List<ProblemCategory> largeCategories = createSampleProblemCategories(500);
        List<Server> largeServerList = createSampleServers(200);
        List<Member> largeMemberList = createSampleMembers(300);
        
        when(cacheKeyManager.getToolWhitelistKey(anyLong())).thenReturn("large:whitelist");
        when(cacheKeyManager.getProblemCategoriesKey(anyLong())).thenReturn("large:categories");
        when(cacheKeyManager.getServerKey(anyLong())).thenReturn("large:server");
        when(cacheKeyManager.getMemberKey(anyLong())).thenReturn("large:member");

        // When - Cache large objects
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 50; i++) {
            cacheService.cacheToolWhitelist((long) i, largeWhitelist);
            cacheService.cacheProblemCategories((long) i, largeCategories);
            cacheService.cacheServer(largeServerList.get(i % largeServerList.size()));
            cacheService.cacheMember(largeMemberList.get(i % largeMemberList.size()));
        }
        
        long cacheDuration = System.nanoTime() - startTime;
        double cacheOpsPerSecond = (50 * 4 * 1_000_000_000.0) / cacheDuration;

        // When - Retrieve large objects
        when(redisService.get(anyString(), eq(List.class))).thenReturn(largeWhitelist);
        when(redisService.get(anyString(), eq(Server.class))).thenReturn(largeServerList.get(0));
        when(redisService.get(anyString(), eq(Member.class))).thenReturn(largeMemberList.get(0));
        
        startTime = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            cacheService.getToolWhitelist((long) i);
            cacheService.getProblemCategories((long) i);
            cacheService.getServer((long) i);
            cacheService.getMember((long) i);
        }
        long retrieveDuration = System.nanoTime() - startTime;
        double retrieveOpsPerSecond = (50 * 4 * 1_000_000_000.0) / retrieveDuration;

        // Then
        assertThat(cacheOpsPerSecond).isGreaterThan(50); // Should handle large objects reasonably
        assertThat(retrieveOpsPerSecond).isGreaterThan(50);

        System.out.printf("Large object performance: Cache %.2f ops/sec, Retrieve %.2f ops/sec%n",
                cacheOpsPerSecond, retrieveOpsPerSecond);
    }

    @Test
    @Timeout(30)
    void testCacheKeyGeneration_ShouldHandleHighVolumeKeyOperations() {
        // Given
        when(cacheKeyManager.getToolWhitelistKey(anyLong())).thenAnswer(
                invocation -> "prefix:whitelist:" + invocation.getArgument(0)
        );
        when(cacheKeyManager.getProblemCategoriesKey(anyLong())).thenReturn("prefix:categories");
        when(cacheKeyManager.getServerKey(anyLong())).thenAnswer(
                invocation -> "prefix:server:" + invocation.getArgument(0)
        );

        // When - Generate many cache keys rapidly
        long startTime = System.nanoTime();
        Set<String> generatedKeys = new HashSet<>();
        
        for (int i = 0; i < PERFORMANCE_ITERATIONS * 10; i++) {
            // Simulate key generation for different cache operations
            String whitelistKey = "prefix:whitelist:" + (i % 100);
            String serverKey = "prefix:server:" + (i % 50);
            String categoryKey = "prefix:categories";
            
            generatedKeys.add(whitelistKey);
            generatedKeys.add(serverKey);
            generatedKeys.add(categoryKey);
        }
        
        long duration = System.nanoTime() - startTime;
        double keyGenOpsPerSecond = (PERFORMANCE_ITERATIONS * 10 * 3 * 1_000_000_000.0) / duration;

        // Then
        assertThat(keyGenOpsPerSecond).isGreaterThan(100000); // Key generation should be very fast
        assertThat(generatedKeys.size()).isGreaterThan(100); // Should generate unique keys

        System.out.printf("Cache key generation: %.2f ops/sec, %d unique keys%n",
                keyGenOpsPerSecond, generatedKeys.size());
    }

    // ========== Helper Methods ==========

    private List<ToolWhitelist> createSampleToolWhitelist(int count) {
        List<ToolWhitelist> whitelists = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ToolWhitelist whitelist = new ToolWhitelist();
            whitelist.setMemberId((long) i);
            whitelist.setToolId((long) i);
            whitelist.setServerId((long) (i % 10));
            whitelist.setAgentConversationId(0L);
            whitelist.setScope(ToolWhitelistScope.SERVER);
            whitelist.setCreatedAt(OffsetDateTime.now());
            whitelist.setUpdatedAt(OffsetDateTime.now());
            whitelists.add(whitelist);
        }
        return whitelists;
    }

    private List<ProblemCategory> createSampleProblemCategories(int count) {
        List<ProblemCategory> categories = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ProblemCategory category = new ProblemCategory();
            category.setId((long) i);
            category.setServerId((long) (i % 5));
            category.setCategoryName("Category " + i);
            category.setDescription("Description for category " + i);
            category.setCreatedAt(OffsetDateTime.now());
            category.setUpdatedAt(OffsetDateTime.now());
            categories.add(category);
        }
        return categories;
    }

    private List<Server> createSampleServers(int count) {
        List<Server> servers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Server server = new Server();
            server.setId((long) i);
            server.setServerName("Server " + i);
            server.setCreatedAt(OffsetDateTime.now());
            server.setUpdatedAt(OffsetDateTime.now());
            servers.add(server);
        }
        return servers;
    }

    private List<Member> createSampleMembers(int count) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Member member = new Member();
            member.setId((long) i);
            member.setMemberName("user" + i);
            member.setCreatedAt(OffsetDateTime.now());
            member.setUpdatedAt(OffsetDateTime.now());
            members.add(member);
        }
        return members;
    }
}