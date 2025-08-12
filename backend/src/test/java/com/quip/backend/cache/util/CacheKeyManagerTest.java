package com.quip.backend.cache.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheKeyManager.
 */
class CacheKeyManagerTest {

    private CacheKeyManager cacheKeyManager;

    @BeforeEach
    void setUp() {
        cacheKeyManager = new CacheKeyManager();
        ReflectionTestUtils.setField(cacheKeyManager, "applicationPrefix", "quip:backend:");
        ReflectionTestUtils.setField(cacheKeyManager, "cacheVersion", "v1");
    }

    // ========== Tool-related Key Generation Tests ==========

    @Test
    void getToolWhitelistKey_ValidServerId_ReturnsCorrectKey() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getToolWhitelistKey(serverId);

        // Then
        assertEquals("quip:backend:v1:tool:whitelist:123", result);
    }

    @Test
    void getToolWhitelistKey_NullServerId_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getToolWhitelistKey(null));
    }

    @Test
    void getToolWhitelistMemberKey_ValidIds_ReturnsCorrectKey() {
        // Given
        Long serverId = 123L;
        Long memberId = 456L;

        // When
        String result = cacheKeyManager.getToolWhitelistMemberKey(serverId, memberId);

        // Then
        assertEquals("quip:backend:v1:tool:whitelist:123:member:456", result);
    }

    @Test
    void getToolWhitelistMemberKey_NullServerId_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getToolWhitelistMemberKey(null, 456L));
    }

    @Test
    void getToolWhitelistMemberKey_NullMemberId_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getToolWhitelistMemberKey(123L, null));
    }

    @Test
    void getToolWhitelistPattern_ValidServerId_ReturnsCorrectPattern() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getToolWhitelistPattern(serverId);

        // Then
        assertEquals("quip:backend:v1:tool:whitelist:123:*", result);
    }

    // ========== Problem-related Key Generation Tests ==========

    @Test
    void getProblemCategoriesKey_ValidServerId_ReturnsCorrectKey() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getProblemCategoriesKey(serverId);

        // Then
        assertEquals("quip:backend:v1:problem:categories:123", result);
    }

    @Test
    void getProblemKey_ValidProblemId_ReturnsCorrectKey() {
        // Given
        Long problemId = 789L;

        // When
        String result = cacheKeyManager.getProblemKey(problemId);

        // Then
        assertEquals("quip:backend:v1:problem:789", result);
    }

    @Test
    void getProblemPattern_ValidServerId_ReturnsCorrectPattern() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getProblemPattern(serverId);

        // Then
        assertEquals("quip:backend:v1:problem:*:123", result);
    }

    // ========== Server-related Key Generation Tests ==========

    @Test
    void getServerKey_ValidServerId_ReturnsCorrectKey() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getServerKey(serverId);

        // Then
        assertEquals("quip:backend:v1:server:123", result);
    }

    @Test
    void getServerRolesKey_ValidServerId_ReturnsCorrectKey() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getServerRolesKey(serverId);

        // Then
        assertEquals("quip:backend:v1:server:123:role", result);
    }

    @Test
    void getServerPattern_ValidServerId_ReturnsCorrectPattern() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getServerPattern(serverId);

        // Then
        assertEquals("quip:backend:v1:server:123:*", result);
    }

    // ========== Member-related Key Generation Tests ==========

    @Test
    void getMemberKey_ValidMemberId_ReturnsCorrectKey() {
        // Given
        Long memberId = 456L;

        // When
        String result = cacheKeyManager.getMemberKey(memberId);

        // Then
        assertEquals("quip:backend:v1:member:456", result);
    }

    @Test
    void getMemberRolesKey_ValidIds_ReturnsCorrectKey() {
        // Given
        Long serverId = 123L;
        Long memberId = 456L;

        // When
        String result = cacheKeyManager.getMemberRolesKey(serverId, memberId);

        // Then
        assertEquals("quip:backend:v1:member:456:server:123:role", result);
    }

    @Test
    void getMemberPattern_ValidServerId_ReturnsCorrectPattern() {
        // Given
        Long serverId = 123L;

        // When
        String result = cacheKeyManager.getMemberPattern(serverId);

        // Then
        assertEquals("quip:backend:v1:member:*:server:123:*", result);
    }

    // ========== Assistant/Session-related Key Generation Tests ==========

    @Test
    void getAssistantSessionKey_ValidSessionId_ReturnsCorrectKey() {
        // Given
        String sessionId = "session-123";

        // When
        String result = cacheKeyManager.getAssistantSessionKey(sessionId);

        // Then
        assertEquals("quip:backend:v1:assistant:session:session-123", result);
    }

    @Test
    void getAssistantSessionKey_NullSessionId_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getAssistantSessionKey(null));
    }

    @Test
    void getAssistantSessionKey_EmptySessionId_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getAssistantSessionKey(""));
    }

    @Test
    void getAssistantSessionPattern_ValidMemberId_ReturnsCorrectPattern() {
        // Given
        Long memberId = 456L;

        // When
        String result = cacheKeyManager.getAssistantSessionPattern(memberId);

        // Then
        assertEquals("quip:backend:v1:assistant:session:*:member:456", result);
    }

    // ========== Generic Key Generation Tests ==========

    @Test
    void getCustomKey_ValidParameters_ReturnsCorrectKey() {
        // Given
        String dataType = "custom";
        String identifier = "test-123";

        // When
        String result = cacheKeyManager.getCustomKey(dataType, identifier);

        // Then
        assertEquals("quip:backend:v1:custom:test-123", result);
    }

    @Test
    void getCustomKey_NullDataType_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getCustomKey(null, "test"));
    }

    @Test
    void getCustomKey_EmptyDataType_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.getCustomKey("", "test"));
    }

    @Test
    void getCustomKey_WithSubComponents_ReturnsCorrectKey() {
        // Given
        String dataType = "custom";
        String identifier = "test-123";
        String subType = "sub";
        String subIdentifier = "456";

        // When
        String result = cacheKeyManager.getCustomKey(dataType, identifier, subType, subIdentifier);

        // Then
        assertEquals("quip:backend:v1:custom:test-123:sub:456", result);
    }

    @Test
    void getCustomPattern_ValidDataType_ReturnsCorrectPattern() {
        // Given
        String dataType = "custom";

        // When
        String result = cacheKeyManager.getCustomPattern(dataType);

        // Then
        assertEquals("quip:backend:v1:custom:*", result);
    }

    // ========== Cache Versioning Tests ==========

    @Test
    void getVersionedKey_ValidParameters_ReturnsCorrectKey() {
        // Given
        String version = "v2";
        String dataType = "test";
        String identifier = "123";

        // When
        String result = cacheKeyManager.getVersionedKey(version, dataType, identifier);

        // Then
        assertEquals("quip:backend:v2:test:123", result);
    }

    @Test
    void getVersionedPattern_ValidVersion_ReturnsCorrectPattern() {
        // Given
        String version = "v2";

        // When
        String result = cacheKeyManager.getVersionedPattern(version);

        // Then
        assertEquals("quip:backend:v2:*", result);
    }

    @Test
    void getCurrentVersion_ReturnsConfiguredVersion() {
        // When
        String result = cacheKeyManager.getCurrentVersion();

        // Then
        assertEquals("v1", result);
    }

    @Test
    void isCurrentVersion_CurrentVersionKey_ReturnsTrue() {
        // Given
        String key = "quip:backend:v1:tool:whitelist:123";

        // When
        boolean result = cacheKeyManager.isCurrentVersion(key);

        // Then
        assertTrue(result);
    }

    @Test
    void isCurrentVersion_OldVersionKey_ReturnsFalse() {
        // Given
        String key = "quip:backend:v0:tool:whitelist:123";

        // When
        boolean result = cacheKeyManager.isCurrentVersion(key);

        // Then
        assertFalse(result);
    }

    @Test
    void isCurrentVersion_InvalidKey_ReturnsFalse() {
        // Given
        String key = "invalid:key:format";

        // When
        boolean result = cacheKeyManager.isCurrentVersion(key);

        // Then
        assertFalse(result);
    }

    @Test
    void extractVersion_ValidKey_ReturnsVersion() {
        // Given
        String key = "quip:backend:v2:tool:whitelist:123";

        // When
        String result = cacheKeyManager.extractVersion(key);

        // Then
        assertEquals("v2", result);
    }

    @Test
    void extractVersion_InvalidKey_ReturnsNull() {
        // Given
        String key = "invalid:key:format";

        // When
        String result = cacheKeyManager.extractVersion(key);

        // Then
        assertNull(result);
    }

    @Test
    void extractVersion_NullKey_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheKeyManager.extractVersion(null));
    }

    // ========== Configuration Tests ==========

    @Test
    void getApplicationPrefix_ReturnsConfiguredPrefix() {
        // When
        String result = cacheKeyManager.getApplicationPrefix();

        // Then
        assertEquals("quip:backend:", result);
    }

    // ========== Edge Cases and Validation Tests ==========

    @Test
    void keyGeneration_WithWhitespaceInIdentifier_TrimsWhitespace() {
        // Given
        String dataType = "test";
        String identifier = "  123  ";

        // When
        String result = cacheKeyManager.getCustomKey(dataType, identifier);

        // Then
        assertEquals("quip:backend:v1:test:123", result);
    }

    @Test
    void keyGeneration_WithDifferentPrefix_UsesConfiguredPrefix() {
        // Given
        ReflectionTestUtils.setField(cacheKeyManager, "applicationPrefix", "myapp");
        String dataType = "test";
        String identifier = "123";

        // When
        String result = cacheKeyManager.getCustomKey(dataType, identifier);

        // Then
        assertEquals("myapp:v1:test:123", result);
    }

    @Test
    void keyGeneration_WithPrefixEndingWithColon_HandlesCorrectly() {
        // Given
        ReflectionTestUtils.setField(cacheKeyManager, "applicationPrefix", "myapp:");
        String dataType = "test";
        String identifier = "123";

        // When
        String result = cacheKeyManager.getCustomKey(dataType, identifier);

        // Then
        assertEquals("myapp:v1:test:123", result);
    }

    @Test
    void keyGeneration_WithEmptyPrefix_HandlesCorrectly() {
        // Given
        ReflectionTestUtils.setField(cacheKeyManager, "applicationPrefix", "");
        String dataType = "test";
        String identifier = "123";

        // When
        String result = cacheKeyManager.getCustomKey(dataType, identifier);

        // Then
        assertEquals("v1:test:123", result);
    }
}