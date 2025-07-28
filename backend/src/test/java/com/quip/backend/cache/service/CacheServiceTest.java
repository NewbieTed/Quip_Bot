package com.quip.backend.cache.service;

import com.quip.backend.cache.util.CacheKeyManager;
import com.quip.backend.member.model.Member;
import com.quip.backend.problem.model.ProblemCategory;
import com.quip.backend.redis.service.RedisService;
import com.quip.backend.server.model.Server;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import com.quip.backend.tool.model.ToolWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheService.
 * Tests all caching operations including tool whitelist, problem categories,
 * server data, member data, and generic caching methods.
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private CacheKeyManager cacheKeyManager;

    @InjectMocks
    private CacheService cacheService;

    private static final String APPLICATION_PREFIX = "quip:backend:";
    private static final String CACHE_VERSION = "v1";
    private static final long DEFAULT_TTL_SECONDS = 3600L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cacheService, "defaultTtlSeconds", DEFAULT_TTL_SECONDS);
    }

    // ========== Tool Whitelist Caching Tests ==========

    @Test
    void cacheToolWhitelist_ShouldCacheSuccessfully() {
        // Given
        Long serverId = 1L;
        List<ToolWhitelist> whitelist = createSampleToolWhitelist();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":tool:whitelist:" + serverId;
        Duration expectedTtl = Duration.ofSeconds(DEFAULT_TTL_SECONDS);
        when(cacheKeyManager.getToolWhitelistKey(serverId)).thenReturn(expectedKey);

        // When
        cacheService.cacheToolWhitelist(serverId, whitelist);

        // Then
        verify(cacheKeyManager).getToolWhitelistKey(serverId);
        verify(redisService).set(expectedKey, whitelist, expectedTtl);
    }

    @Test
    void cacheToolWhitelist_WithNullServerId_ShouldThrowException() {
        // Given
        List<ToolWhitelist> whitelist = createSampleToolWhitelist();

        // When & Then
        assertThatThrownBy(() -> cacheService.cacheToolWhitelist(null, whitelist))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Server ID cannot be null");
    }

    @Test
    void cacheToolWhitelist_WithNullWhitelist_ShouldThrowException() {
        // Given
        Long serverId = 1L;

        // When & Then
        assertThatThrownBy(() -> cacheService.cacheToolWhitelist(serverId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Whitelist cannot be null");
    }

    @Test
    void getToolWhitelist_ShouldReturnCachedData() {
        // Given
        Long serverId = 1L;
        List<ToolWhitelist> expectedWhitelist = createSampleToolWhitelist();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":tool:whitelist:" + serverId;
        when(cacheKeyManager.getToolWhitelistKey(serverId)).thenReturn(expectedKey);
        when(redisService.get(expectedKey, List.class)).thenReturn(expectedWhitelist);

        // When
        List<ToolWhitelist> result = cacheService.getToolWhitelist(serverId);

        // Then
        assertThat(result).isEqualTo(expectedWhitelist);
        verify(cacheKeyManager).getToolWhitelistKey(serverId);
        verify(redisService).get(expectedKey, List.class);
    }

    @Test
    void getToolWhitelist_WithNullServerId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> cacheService.getToolWhitelist(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Server ID cannot be null");
    }

    @Test
    void evictToolWhitelist_ShouldEvictSuccessfully() {
        // Given
        Long serverId = 1L;
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":tool:whitelist:" + serverId;
        when(cacheKeyManager.getToolWhitelistKey(serverId)).thenReturn(expectedKey);
        when(redisService.delete(expectedKey)).thenReturn(true);

        // When
        boolean result = cacheService.evictToolWhitelist(serverId);

        // Then
        assertThat(result).isTrue();
        verify(cacheKeyManager).getToolWhitelistKey(serverId);
        verify(redisService).delete(expectedKey);
    }

    @Test
    void cacheToolWhitelistForMember_ShouldCacheSuccessfully() {
        // Given
        Long serverId = 1L;
        Long memberId = 2L;
        List<ToolWhitelist> whitelist = createSampleToolWhitelist();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":tool:whitelist:" + serverId + ":member:" + memberId;
        Duration expectedTtl = Duration.ofSeconds(DEFAULT_TTL_SECONDS);
        when(cacheKeyManager.getToolWhitelistMemberKey(serverId, memberId)).thenReturn(expectedKey);

        // When
        cacheService.cacheToolWhitelistForMember(serverId, memberId, whitelist);

        // Then
        verify(cacheKeyManager).getToolWhitelistMemberKey(serverId, memberId);
        verify(redisService).set(expectedKey, whitelist, expectedTtl);
    }

    @Test
    void getToolWhitelistForMember_ShouldReturnCachedData() {
        // Given
        Long serverId = 1L;
        Long memberId = 2L;
        List<ToolWhitelist> expectedWhitelist = createSampleToolWhitelist();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":tool:whitelist:" + serverId + ":member:" + memberId;
        when(cacheKeyManager.getToolWhitelistMemberKey(serverId, memberId)).thenReturn(expectedKey);
        when(redisService.get(expectedKey, List.class)).thenReturn(expectedWhitelist);

        // When
        List<ToolWhitelist> result = cacheService.getToolWhitelistForMember(serverId, memberId);

        // Then
        assertThat(result).isEqualTo(expectedWhitelist);
        verify(cacheKeyManager).getToolWhitelistMemberKey(serverId, memberId);
        verify(redisService).get(expectedKey, List.class);
    }

    // ========== Problem Categories Caching Tests ==========

    @Test
    void cacheProblemCategories_ShouldCacheSuccessfully() {
        // Given
        Long serverId = 1L;
        List<ProblemCategory> categories = createSampleProblemCategories();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":problem:categories:" + serverId;
        Duration expectedTtl = Duration.ofSeconds(DEFAULT_TTL_SECONDS * 24); // 24 hours
        when(cacheKeyManager.getProblemCategoriesKey(serverId)).thenReturn(expectedKey);

        // When
        cacheService.cacheProblemCategories(serverId, categories);

        // Then
        verify(cacheKeyManager).getProblemCategoriesKey(serverId);
        verify(redisService).set(expectedKey, categories, expectedTtl);
    }

    @Test
    void getProblemCategories_ShouldReturnCachedData() {
        // Given
        Long serverId = 1L;
        List<ProblemCategory> expectedCategories = createSampleProblemCategories();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":problem:categories:" + serverId;
        when(cacheKeyManager.getProblemCategoriesKey(serverId)).thenReturn(expectedKey);
        when(redisService.get(expectedKey, List.class)).thenReturn(expectedCategories);

        // When
        List<ProblemCategory> result = cacheService.getProblemCategories(serverId);

        // Then
        assertThat(result).isEqualTo(expectedCategories);
        verify(cacheKeyManager).getProblemCategoriesKey(serverId);
        verify(redisService).get(expectedKey, List.class);
    }

    @Test
    void evictProblemCategories_ShouldEvictSuccessfully() {
        // Given
        Long serverId = 1L;
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":problem:categories:" + serverId;
        when(cacheKeyManager.getProblemCategoriesKey(serverId)).thenReturn(expectedKey);
        when(redisService.delete(expectedKey)).thenReturn(true);

        // When
        boolean result = cacheService.evictProblemCategories(serverId);

        // Then
        assertThat(result).isTrue();
        verify(cacheKeyManager).getProblemCategoriesKey(serverId);
        verify(redisService).delete(expectedKey);
    }

    // ========== Server Data Caching Tests ==========

    @Test
    void cacheServer_ShouldCacheSuccessfully() {
        // Given
        Server server = createSampleServer();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":server:" + server.getId();
        Duration expectedTtl = Duration.ofSeconds(DEFAULT_TTL_SECONDS * 6); // 6 hours
        when(cacheKeyManager.getServerKey(server.getId())).thenReturn(expectedKey);

        // When
        cacheService.cacheServer(server);

        // Then
        verify(cacheKeyManager).getServerKey(server.getId());
        verify(redisService).set(expectedKey, server, expectedTtl);
    }

    @Test
    void cacheServer_WithNullServer_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> cacheService.cacheServer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Server cannot be null");
    }

    @Test
    void cacheServer_WithNullServerId_ShouldThrowException() {
        // Given
        Server server = Server.builder().serverName("Test Server").build();

        // When & Then
        assertThatThrownBy(() -> cacheService.cacheServer(server))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Server ID cannot be null");
    }

    @Test
    void getServer_ShouldReturnCachedData() {
        // Given
        Long serverId = 1L;
        Server expectedServer = createSampleServer();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":server:" + serverId;
        when(cacheKeyManager.getServerKey(serverId)).thenReturn(expectedKey);
        when(redisService.get(expectedKey, Server.class)).thenReturn(expectedServer);

        // When
        Server result = cacheService.getServer(serverId);

        // Then
        assertThat(result).isEqualTo(expectedServer);
        verify(cacheKeyManager).getServerKey(serverId);
        verify(redisService).get(expectedKey, Server.class);
    }

    @Test
    void evictServer_ShouldEvictSuccessfully() {
        // Given
        Long serverId = 1L;
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":server:" + serverId;
        when(cacheKeyManager.getServerKey(serverId)).thenReturn(expectedKey);
        when(redisService.delete(expectedKey)).thenReturn(true);

        // When
        boolean result = cacheService.evictServer(serverId);

        // Then
        assertThat(result).isTrue();
        verify(cacheKeyManager).getServerKey(serverId);
        verify(redisService).delete(expectedKey);
    }

    // ========== Member Data Caching Tests ==========

    @Test
    void cacheMember_ShouldCacheSuccessfully() {
        // Given
        Member member = createSampleMember();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":member:" + member.getId();
        Duration expectedTtl = Duration.ofSeconds(DEFAULT_TTL_SECONDS * 2); // 2 hours
        when(cacheKeyManager.getMemberKey(member.getId())).thenReturn(expectedKey);

        // When
        cacheService.cacheMember(member);

        // Then
        verify(cacheKeyManager).getMemberKey(member.getId());
        verify(redisService).set(expectedKey, member, expectedTtl);
    }

    @Test
    void cacheMember_WithNullMember_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> cacheService.cacheMember(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Member cannot be null");
    }

    @Test
    void getMember_ShouldReturnCachedData() {
        // Given
        Long memberId = 1L;
        Member expectedMember = createSampleMember();
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":member:" + memberId;
        when(cacheKeyManager.getMemberKey(memberId)).thenReturn(expectedKey);
        when(redisService.get(expectedKey, Member.class)).thenReturn(expectedMember);

        // When
        Member result = cacheService.getMember(memberId);

        // Then
        assertThat(result).isEqualTo(expectedMember);
        verify(cacheKeyManager).getMemberKey(memberId);
        verify(redisService).get(expectedKey, Member.class);
    }

    @Test
    void evictMember_ShouldEvictSuccessfully() {
        // Given
        Long memberId = 1L;
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":member:" + memberId;
        when(cacheKeyManager.getMemberKey(memberId)).thenReturn(expectedKey);
        when(redisService.delete(expectedKey)).thenReturn(true);

        // When
        boolean result = cacheService.evictMember(memberId);

        // Then
        assertThat(result).isTrue();
        verify(cacheKeyManager).getMemberKey(memberId);
        verify(redisService).delete(expectedKey);
    }

    // ========== Generic Caching Tests ==========

    @Test
    void cacheWithTTL_ShouldCacheSuccessfully() {
        // Given
        String keyPrefix = "test";
        String identifier = "123";
        String data = "test data";
        Duration ttl = Duration.ofMinutes(30);
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":" + keyPrefix + ":" + identifier;
        when(cacheKeyManager.getCustomKey(keyPrefix, identifier)).thenReturn(expectedKey);

        // When
        cacheService.cacheWithTTL(keyPrefix, identifier, data, ttl);

        // Then
        verify(cacheKeyManager).getCustomKey(keyPrefix, identifier);
        verify(redisService).set(expectedKey, data, ttl);
    }

    @Test
    void cacheWithTTL_WithNullKeyPrefix_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> cacheService.cacheWithTTL(null, "123", "data", Duration.ofMinutes(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Key prefix cannot be null or empty");
    }

    @Test
    void getFromCache_ShouldReturnCachedData() {
        // Given
        String keyPrefix = "test";
        String identifier = "123";
        String expectedData = "test data";
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":" + keyPrefix + ":" + identifier;
        when(cacheKeyManager.getCustomKey(keyPrefix, identifier)).thenReturn(expectedKey);
        when(redisService.get(expectedKey, String.class)).thenReturn(expectedData);

        // When
        String result = cacheService.getFromCache(keyPrefix, identifier, String.class);

        // Then
        assertThat(result).isEqualTo(expectedData);
        verify(cacheKeyManager).getCustomKey(keyPrefix, identifier);
        verify(redisService).get(expectedKey, String.class);
    }

    @Test
    void evictFromCache_ShouldEvictSuccessfully() {
        // Given
        String keyPrefix = "test";
        String identifier = "123";
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":" + keyPrefix + ":" + identifier;
        when(cacheKeyManager.getCustomKey(keyPrefix, identifier)).thenReturn(expectedKey);
        when(redisService.delete(expectedKey)).thenReturn(true);

        // When
        boolean result = cacheService.evictFromCache(keyPrefix, identifier);

        // Then
        assertThat(result).isTrue();
        verify(cacheKeyManager).getCustomKey(keyPrefix, identifier);
        verify(redisService).delete(expectedKey);
    }

    @Test
    void evictByPattern_ShouldEvictMatchingKeys() {
        // Given
        String keyPrefix = "test";
        String expectedPattern = APPLICATION_PREFIX + CACHE_VERSION + ":" + keyPrefix + ":*";
        when(cacheKeyManager.getCustomPattern(keyPrefix)).thenReturn(expectedPattern);
        when(redisService.deleteByPattern(expectedPattern)).thenReturn(5L);

        // When
        long result = cacheService.evictByPattern(keyPrefix);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(cacheKeyManager).getCustomPattern(keyPrefix);
        verify(redisService).deleteByPattern(expectedPattern);
    }

    @Test
    void existsInCache_ShouldReturnExistenceStatus() {
        // Given
        String keyPrefix = "test";
        String identifier = "123";
        String expectedKey = APPLICATION_PREFIX + CACHE_VERSION + ":" + keyPrefix + ":" + identifier;
        when(cacheKeyManager.getCustomKey(keyPrefix, identifier)).thenReturn(expectedKey);
        when(redisService.exists(expectedKey)).thenReturn(true);

        // When
        boolean result = cacheService.existsInCache(keyPrefix, identifier);

        // Then
        assertThat(result).isTrue();
        verify(cacheKeyManager).getCustomKey(keyPrefix, identifier);
        verify(redisService).exists(expectedKey);
    }

    // ========== Utility Tests ==========

    @Test
    void getDefaultTtlSeconds_ShouldReturnConfiguredValue() {
        // When
        long result = cacheService.getDefaultTtlSeconds();

        // Then
        assertThat(result).isEqualTo(DEFAULT_TTL_SECONDS);
    }

    @Test
    void getKeyPrefix_ShouldReturnConfiguredValue() {
        // Given
        when(cacheKeyManager.getApplicationPrefix()).thenReturn(APPLICATION_PREFIX);

        // When
        String result = cacheService.getKeyPrefix();

        // Then
        assertThat(result).isEqualTo(APPLICATION_PREFIX);
        verify(cacheKeyManager).getApplicationPrefix();
    }

    @Test
    void getCurrentCacheVersion_ShouldReturnConfiguredValue() {
        // Given
        when(cacheKeyManager.getCurrentVersion()).thenReturn(CACHE_VERSION);

        // When
        String result = cacheService.getCurrentCacheVersion();

        // Then
        assertThat(result).isEqualTo(CACHE_VERSION);
        verify(cacheKeyManager).getCurrentVersion();
    }

    @Test
    void evictAllToolWhitelistForServer_ShouldEvictMatchingKeys() {
        // Given
        Long serverId = 1L;
        String expectedPattern = APPLICATION_PREFIX + CACHE_VERSION + ":tool:whitelist:" + serverId + ":*";
        when(cacheKeyManager.getToolWhitelistPattern(serverId)).thenReturn(expectedPattern);
        when(redisService.deleteByPattern(expectedPattern)).thenReturn(3L);

        // When
        long result = cacheService.evictAllToolWhitelistForServer(serverId);

        // Then
        assertThat(result).isEqualTo(3L);
        verify(cacheKeyManager).getToolWhitelistPattern(serverId);
        verify(redisService).deleteByPattern(expectedPattern);
    }

    @Test
    void evictAllServerData_ShouldEvictMatchingKeys() {
        // Given
        Long serverId = 1L;
        String expectedPattern = APPLICATION_PREFIX + CACHE_VERSION + ":server:" + serverId + ":*";
        when(cacheKeyManager.getServerPattern(serverId)).thenReturn(expectedPattern);
        when(redisService.deleteByPattern(expectedPattern)).thenReturn(2L);

        // When
        long result = cacheService.evictAllServerData(serverId);

        // Then
        assertThat(result).isEqualTo(2L);
        verify(cacheKeyManager).getServerPattern(serverId);
        verify(redisService).deleteByPattern(expectedPattern);
    }

    @Test
    void evictAllMemberDataForServer_ShouldEvictMatchingKeys() {
        // Given
        Long serverId = 1L;
        String expectedPattern = APPLICATION_PREFIX + CACHE_VERSION + ":member:*:server:" + serverId + ":*";
        when(cacheKeyManager.getMemberPattern(serverId)).thenReturn(expectedPattern);
        when(redisService.deleteByPattern(expectedPattern)).thenReturn(4L);

        // When
        long result = cacheService.evictAllMemberDataForServer(serverId);

        // Then
        assertThat(result).isEqualTo(4L);
        verify(cacheKeyManager).getMemberPattern(serverId);
        verify(redisService).deleteByPattern(expectedPattern);
    }

    // ========== Helper Methods ==========

    private List<ToolWhitelist> createSampleToolWhitelist() {
        ToolWhitelist whitelist1 = ToolWhitelist.builder()
                .memberId(1L)
                .toolId(1L)
                .serverId(1L)
                .agentConversationId(0L)
                .scope(ToolWhitelistScope.SERVER)
                .createdBy(1L)
                .updatedBy(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ToolWhitelist whitelist2 = ToolWhitelist.builder()
                .memberId(1L)
                .toolId(2L)
                .serverId(1L)
                .agentConversationId(0L)
                .scope(ToolWhitelistScope.GLOBAL)
                .createdBy(1L)
                .updatedBy(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return Arrays.asList(whitelist1, whitelist2);
    }

    private List<ProblemCategory> createSampleProblemCategories() {
        ProblemCategory category1 = ProblemCategory.builder()
                .id(1L)
                .serverId(1L)
                .categoryName("Technical Issues")
                .description("Problems related to technical difficulties")
                .createdBy(1L)
                .updatedBy(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ProblemCategory category2 = ProblemCategory.builder()
                .id(2L)
                .serverId(1L)
                .categoryName("User Support")
                .description("Problems related to user support requests")
                .createdBy(1L)
                .updatedBy(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return Arrays.asList(category1, category2);
    }

    private Server createSampleServer() {
        return Server.builder()
                .id(1L)
                .serverName("Test Server")
                .createdBy(1L)
                .updatedBy(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private Member createSampleMember() {
        return Member.builder()
                .id(1L)
                .memberName("Test Member")
                .createdBy(1L)
                .updatedBy(1L)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}