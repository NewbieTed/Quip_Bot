package com.quip.backend.cache.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility class for managing cache key generation and versioning.
 * <p>
 * This class provides consistent key naming patterns with application prefix,
 * key generation utilities for different data types, and cache versioning support
 * for data model changes.
 * </p>
 * <p>
 * Key Pattern: {applicationPrefix}:{version}:{dataType}:{identifier}[:{subType}:{subIdentifier}]
 * </p>
 * <p>
 * Examples:
 * - quip:backend:v1:tool:whitelist:123
 * - quip:backend:v1:tool:whitelist:123:member:456
 * - quip:backend:v1:problem:categories:789
 * - quip:backend:v1:server:123
 * - quip:backend:v1:member:456
 * </p>
 */
@Slf4j
@Component
public class CacheKeyManager {

    @Value("${app.redis.key-prefix:quip:backend:}")
    private String applicationPrefix;

    @Value("${app.redis.cache-version:v1}")
    private String cacheVersion;

    // ========== Data Type Constants ==========

    public static final String DATA_TYPE_TOOL = "tool";
    public static final String DATA_TYPE_PROBLEM = "problem";
    public static final String DATA_TYPE_SERVER = "server";
    public static final String DATA_TYPE_MEMBER = "member";
    public static final String DATA_TYPE_ASSISTANT = "assistant";
    public static final String DATA_TYPE_SESSION = "session";

    // ========== Sub Type Constants ==========

    public static final String SUB_TYPE_WHITELIST = "whitelist";
    public static final String SUB_TYPE_CATEGORIES = "categories";
    public static final String SUB_TYPE_MEMBER = "member";
    public static final String SUB_TYPE_CHANNEL = "channel";
    public static final String SUB_TYPE_ROLE = "role";

    // ========== Tool-related Key Generation ==========

    /**
     * Generates a cache key for tool whitelist data for a specific server.
     *
     * @param serverId the server ID
     * @return the cache key for tool whitelist
     * @throws IllegalArgumentException if serverId is null
     */
    public String getToolWhitelistKey(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_TOOL, SUB_TYPE_WHITELIST, serverId.toString());
    }

    /**
     * Generates a cache key for tool whitelist data for a specific member in a server.
     *
     * @param serverId the server ID
     * @param memberId the member ID
     * @return the cache key for member-specific tool whitelist
     * @throws IllegalArgumentException if serverId or memberId is null
     */
    public String getToolWhitelistMemberKey(Long serverId, Long memberId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        return buildKey(DATA_TYPE_TOOL, SUB_TYPE_WHITELIST, serverId.toString(), 
                       SUB_TYPE_MEMBER, memberId.toString());
    }

    /**
     * Generates a cache key pattern for all tool whitelist entries for a server.
     *
     * @param serverId the server ID
     * @return the cache key pattern for all tool whitelist entries
     * @throws IllegalArgumentException if serverId is null
     */
    public String getToolWhitelistPattern(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_TOOL, SUB_TYPE_WHITELIST, serverId.toString(), "*");
    }

    // ========== Problem-related Key Generation ==========

    /**
     * Generates a cache key for problem categories for a specific server.
     *
     * @param serverId the server ID
     * @return the cache key for problem categories
     * @throws IllegalArgumentException if serverId is null
     */
    public String getProblemCategoriesKey(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_PROBLEM, SUB_TYPE_CATEGORIES, serverId.toString());
    }

    /**
     * Generates a cache key for a specific problem.
     *
     * @param problemId the problem ID
     * @return the cache key for the problem
     * @throws IllegalArgumentException if problemId is null
     */
    public String getProblemKey(Long problemId) {
        if (problemId == null) {
            throw new IllegalArgumentException("Problem ID cannot be null");
        }
        return buildKey(DATA_TYPE_PROBLEM, problemId.toString());
    }

    /**
     * Generates a cache key pattern for all problems in a server.
     *
     * @param serverId the server ID
     * @return the cache key pattern for all problems
     * @throws IllegalArgumentException if serverId is null
     */
    public String getProblemPattern(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_PROBLEM, "*", serverId.toString());
    }

    // ========== Server-related Key Generation ==========

    /**
     * Generates a cache key for server data.
     *
     * @param serverId the server ID
     * @return the cache key for server data
     * @throws IllegalArgumentException if serverId is null
     */
    public String getServerKey(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_SERVER, serverId.toString());
    }

    /**
     * Generates a cache key for server roles.
     *
     * @param serverId the server ID
     * @return the cache key for server roles
     * @throws IllegalArgumentException if serverId is null
     */
    public String getServerRolesKey(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_SERVER, serverId.toString(), SUB_TYPE_ROLE);
    }

    /**
     * Generates a cache key pattern for all server-related data.
     *
     * @param serverId the server ID
     * @return the cache key pattern for all server data
     * @throws IllegalArgumentException if serverId is null
     */
    public String getServerPattern(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_SERVER, serverId.toString(), "*");
    }

    // ========== Member-related Key Generation ==========

    /**
     * Generates a cache key for member data.
     *
     * @param memberId the member ID
     * @return the cache key for member data
     * @throws IllegalArgumentException if memberId is null
     */
    public String getMemberKey(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        return buildKey(DATA_TYPE_MEMBER, memberId.toString());
    }

    /**
     * Generates a cache key for member roles in a server.
     *
     * @param serverId the server ID
     * @param memberId the member ID
     * @return the cache key for member roles
     * @throws IllegalArgumentException if serverId or memberId is null
     */
    public String getMemberRolesKey(Long serverId, Long memberId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        return buildKey(DATA_TYPE_MEMBER, memberId.toString(), DATA_TYPE_SERVER, serverId.toString(), SUB_TYPE_ROLE);
    }

    /**
     * Generates a cache key pattern for all member data in a server.
     *
     * @param serverId the server ID
     * @return the cache key pattern for all member data
     * @throws IllegalArgumentException if serverId is null
     */
    public String getMemberPattern(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        return buildKey(DATA_TYPE_MEMBER, "*", DATA_TYPE_SERVER, serverId.toString(), "*");
    }

    // ========== Assistant/Session-related Key Generation ==========

    /**
     * Generates a cache key for assistant session data.
     *
     * @param sessionId the session ID
     * @return the cache key for assistant session
     * @throws IllegalArgumentException if sessionId is null or empty
     */
    public String getAssistantSessionKey(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        return buildKey(DATA_TYPE_ASSISTANT, DATA_TYPE_SESSION, sessionId.trim());
    }

    /**
     * Generates a cache key pattern for all assistant sessions for a member.
     *
     * @param memberId the member ID
     * @return the cache key pattern for member sessions
     * @throws IllegalArgumentException if memberId is null
     */
    public String getAssistantSessionPattern(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        return buildKey(DATA_TYPE_ASSISTANT, DATA_TYPE_SESSION, "*", SUB_TYPE_MEMBER, memberId.toString());
    }

    // ========== Generic Key Generation ==========

    /**
     * Generates a cache key for custom data types.
     *
     * @param dataType   the data type
     * @param identifier the unique identifier
     * @return the cache key
     * @throws IllegalArgumentException if dataType or identifier is null or empty
     */
    public String getCustomKey(String dataType, String identifier) {
        if (dataType == null || dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("Data type cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        return buildKey(dataType.trim(), identifier.trim());
    }

    /**
     * Generates a cache key for custom data types with sub-components.
     *
     * @param dataType    the data type
     * @param identifier  the unique identifier
     * @param subType     the sub-type
     * @param subIdentifier the sub-identifier
     * @return the cache key
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public String getCustomKey(String dataType, String identifier, String subType, String subIdentifier) {
        if (dataType == null || dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("Data type cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (subType == null || subType.trim().isEmpty()) {
            throw new IllegalArgumentException("Sub-type cannot be null or empty");
        }
        if (subIdentifier == null || subIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Sub-identifier cannot be null or empty");
        }
        return buildKey(dataType.trim(), identifier.trim(), subType.trim(), subIdentifier.trim());
    }

    /**
     * Generates a cache key pattern for custom data types.
     *
     * @param dataType the data type
     * @return the cache key pattern
     * @throws IllegalArgumentException if dataType is null or empty
     */
    public String getCustomPattern(String dataType) {
        if (dataType == null || dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("Data type cannot be null or empty");
        }
        return buildKey(dataType.trim(), "*");
    }

    // ========== Cache Versioning Support ==========

    /**
     * Generates a versioned cache key for data model migration scenarios.
     *
     * @param version    the cache version
     * @param dataType   the data type
     * @param identifier the unique identifier
     * @return the versioned cache key
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public String getVersionedKey(String version, String dataType, String identifier) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        if (dataType == null || dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("Data type cannot be null or empty");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        return buildVersionedKey(version.trim(), dataType.trim(), identifier.trim());
    }

    /**
     * Generates a cache key pattern for a specific version.
     *
     * @param version the cache version
     * @return the versioned cache key pattern
     * @throws IllegalArgumentException if version is null or empty
     */
    public String getVersionedPattern(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        return buildVersionedKey(version.trim(), "*");
    }

    /**
     * Gets the current cache version.
     *
     * @return the current cache version
     */
    public String getCurrentVersion() {
        return cacheVersion;
    }

    /**
     * Checks if a cache key belongs to the current version.
     *
     * @param key the cache key to check
     * @return true if the key belongs to the current version, false otherwise
     * @throws IllegalArgumentException if key is null or empty
     */
    public boolean isCurrentVersion(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        
        String expectedPrefix = normalizePrefix(applicationPrefix) + cacheVersion + ":";
        return key.startsWith(expectedPrefix);
    }

    /**
     * Extracts the version from a cache key.
     *
     * @param key the cache key
     * @return the version extracted from the key, or null if not found
     * @throws IllegalArgumentException if key is null or empty
     */
    public String extractVersion(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        
        String normalizedPrefix = normalizePrefix(applicationPrefix);
        if (!key.startsWith(normalizedPrefix)) {
            return null;
        }
        
        String withoutPrefix = key.substring(normalizedPrefix.length());
        int versionEnd = withoutPrefix.indexOf(':');
        
        return versionEnd > 0 ? withoutPrefix.substring(0, versionEnd) : null;
    }

    // ========== Utility Methods ==========

    /**
     * Builds a cache key using the application prefix, version, and provided components.
     *
     * @param components the key components to join
     * @return the complete cache key
     */
    private String buildKey(String... components) {
        return buildVersionedKey(cacheVersion, components);
    }

    /**
     * Builds a versioned cache key using the application prefix, specified version, and components.
     *
     * @param version    the cache version
     * @param components the key components to join
     * @return the complete versioned cache key
     */
    private String buildVersionedKey(String version, String... components) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Add application prefix
        keyBuilder.append(normalizePrefix(applicationPrefix));
        
        // Add version
        keyBuilder.append(version).append(":");
        
        // Add components
        String joinedComponents = Arrays.stream(components)
                .filter(component -> component != null && !component.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining(":"));
        
        keyBuilder.append(joinedComponents);
        
        String finalKey = keyBuilder.toString();
        log.trace("Generated cache key: {}", finalKey);
        return finalKey;
    }

    /**
     * Normalizes the application prefix to ensure it ends with a colon.
     *
     * @param prefix the prefix to normalize
     * @return the normalized prefix
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return "";
        }
        
        String normalized = prefix.trim();
        return normalized.endsWith(":") ? normalized : normalized + ":";
    }

    /**
     * Gets the configured application prefix.
     *
     * @return the application prefix
     */
    public String getApplicationPrefix() {
        return applicationPrefix;
    }

    /**
     * Validates that all provided components are non-null and non-empty.
     *
     * @param components the components to validate
     * @throws IllegalArgumentException if any component is null or empty
     */
    private void validateComponents(String... components) {
        for (int i = 0; i < components.length; i++) {
            if (components[i] == null || components[i].trim().isEmpty()) {
                throw new IllegalArgumentException("Component at index " + i + " cannot be null or empty");
            }
        }
    }
}