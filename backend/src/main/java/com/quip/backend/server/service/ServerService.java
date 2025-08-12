package com.quip.backend.server.service;


import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import com.quip.backend.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for server-related operations.
 * <p>
 * This service provides functionality to validate and retrieve server entities,
 * ensuring they exist and are properly referenced throughout the application.
 * It handles server validation for various operations and provides consistent
 * error handling for server-related issues.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {
    private final ServerMapper serverMapper;


    /**
     * Validates that a server exists for a given operation with caching.
     * <p>
     * This method checks if the provided server ID is valid and refers to an existing server.
     * It's used during operations that require a valid server reference.
     * Results are cached to improve performance for frequently validated servers.
     * </p>
     *
     * @param serverId The ID of the server to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return The validated Server entity
     * @throws ValidationException If the server ID is null or refers to a non-existent server
     * @throws IllegalArgumentException If the operation parameter is null
     */
    @Cacheable(value = CacheConfiguration.SERVER_DATA_CACHE, 
               key = "#serverId")
    public Server validateServer(Long serverId, String operation) {
        // Validate input parameters
        if (serverId == null) {
            throw new ValidationException(operation, "serverId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        
        log.debug("Validating server from database for serverId: {}", serverId);
        
        // Attempt to retrieve the server from the database
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            throw new ValidationException(operation, "serverId", "must refer to an existing server");
        }
        
        log.info("Validated serverId: {}", serverId);
        return server;
    }

    /**
     * Asserts that a server exists with the given ID with caching.
     * <p>
     * This method is similar to validateServer but throws an EntityNotFoundException
     * instead of a ValidationException when the server doesn't exist. It's used in contexts
     * where a missing server is considered a system error rather than a validation issue.
     * Results are cached to improve performance for frequently checked servers.
     * </p>
     *
     * @param serverId The ID of the server to check
     * @return The Server entity if it exists
     * @throws IllegalArgumentException If the server ID is null
     * @throws EntityNotFoundException If no server exists with the given ID
     */
    @Cacheable(value = CacheConfiguration.SERVER_DATA_CACHE, 
               key = "'assert:' + #serverId")
    public Server assertServerExist(Long serverId) {
        // Check for null server ID
        if (serverId == null) {
            throw new IllegalArgumentException("Parameter 'serverId' must not be null");
        }
        
        log.debug("Asserting server existence from database for serverId: {}", serverId);
        
        // Attempt to retrieve the server
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            // Throw system-level exception for missing server
            throw new EntityNotFoundException("Server not found for serverId: " + serverId);
        }
        
        log.info("ServerId existence validated: {}", serverId);
        return server;
    }

    /**
     * Retrieves a server by ID with caching.
     * <p>
     * This method provides direct access to server data by ID
     * with caching support for improved performance.
     * </p>
     *
     * @param serverId the server ID
     * @return the Server entity
     */
    @Cacheable(value = CacheConfiguration.SERVER_DATA_CACHE, 
               key = "'server:' + #serverId")
    public Server getServerById(Long serverId) {
        log.debug("Retrieving server from database for serverId: {}", serverId);
        return serverMapper.selectById(serverId);
    }

    /**
     * Evicts server cache for a specific server.
     * <p>
     * This method removes cached server data for a server,
     * typically called when server data is modified.
     * </p>
     *
     * @param serverId the server ID to evict cache for
     */
    @CacheEvict(value = CacheConfiguration.SERVER_DATA_CACHE, 
                allEntries = true)
    public void evictServerCache(Long serverId) {
        log.debug("Evicting server cache for serverId: {}", serverId);
    }

    /**
     * Evicts all server cache entries.
     * <p>
     * This method removes all cached server data,
     * typically called during bulk operations or system maintenance.
     * </p>
     */
    @CacheEvict(value = CacheConfiguration.SERVER_DATA_CACHE, 
                allEntries = true)
    public void evictAllServerCache() {
        log.debug("Evicting all server cache entries");
    }

}