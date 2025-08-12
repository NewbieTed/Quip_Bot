package com.quip.backend.cache.service;

import com.quip.backend.problem.service.ProblemCategoryService;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for warming up caches with static data during application startup.
 * <p>
 * This service pre-populates caches with frequently accessed static data to improve
 * initial response times and reduce database load. Cache warming is performed
 * asynchronously after the application is fully started.
 * </p>
 * <p>
 * The service focuses on warming caches for:
 * - Problem categories (static data with long TTL)
 * - Other static configuration data as needed
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmingService {

    private final ProblemCategoryService problemCategoryService;
    private final ServerMapper serverMapper;

    /**
     * Warms up caches with static data after application startup.
     * <p>
     * This method is triggered when the application is fully ready and performs
     * cache warming asynchronously to avoid blocking the startup process.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmCaches() {
        log.info("Starting cache warming process...");
        
        try {
            warmProblemCategoriesCache();
            log.info("Cache warming completed successfully");
        } catch (Exception e) {
            log.error("Error during cache warming process", e);
            // Don't rethrow - cache warming failure shouldn't prevent application startup
        }
    }

    /**
     * Warms up the problem categories cache for all servers.
     * <p>
     * This method retrieves all servers and pre-populates the problem categories
     * cache for each server. Problem categories are considered static data and
     * benefit significantly from cache warming.
     * </p>
     */
    private void warmProblemCategoriesCache() {
        log.debug("Warming problem categories cache...");
        
        try {
            // Get all servers to warm their problem categories cache
            List<Server> servers = serverMapper.selectList(null);
            
            if (servers == null || servers.isEmpty()) {
                log.debug("No discord servers found, skipping problem categories cache warming");
                return;
            }
            
            int warmedServers = 0;
            for (Server server : servers) {
                try {
                    // This will populate the cache through the @Cacheable annotation
                    problemCategoryService.getProblemCategoriesByServerId(server.getId());
                    warmedServers++;
                    log.debug("Warmed problem categories cache for server: {} ({})", 
                             server.getId(), server.getServerName());
                } catch (Exception e) {
                    log.warn("Failed to warm problem categories cache for server: {} ({})", 
                            server.getId(), server.getServerName(), e);
                    // Continue with other servers
                }
            }
            
            log.info("Problem categories cache warming completed for {}/{} servers", 
                    warmedServers, servers.size());
            
        } catch (Exception e) {
            log.error("Failed to warm problem categories cache", e);
        }
    }

    /**
     * Manually triggers cache warming for problem categories.
     * <p>
     * This method can be called programmatically to refresh the problem categories
     * cache, for example after bulk data updates or during maintenance operations.
     * </p>
     */
    public void manualWarmProblemCategoriesCache() {
        log.info("Manual cache warming triggered for problem categories");
        warmProblemCategoriesCache();
    }

    /**
     * Warms cache for a specific server's problem categories.
     * <p>
     * This method can be used to warm the cache for a specific server,
     * useful when a new server is added or when server-specific cache
     * needs to be refreshed.
     * </p>
     *
     * @param serverId the server ID to warm cache for
     * @throws IllegalArgumentException if serverId is null
     */
    public void warmProblemCategoriesCacheForServer(Long serverId) {
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        
        log.debug("Warming problem categories cache for server: {}", serverId);
        
        try {
            // This will populate the cache through the @Cacheable annotation
            problemCategoryService.getProblemCategoriesByServerId(serverId);
            log.info("Successfully warmed problem categories cache for server: {}", serverId);
        } catch (Exception e) {
            log.error("Failed to warm problem categories cache for server: {}", serverId, e);
            throw e;
        }
    }
}