package com.quip.backend.problem.service;

import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;

import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemCategoryRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemCategoryResponseDtoMapper;
import com.quip.backend.problem.model.ProblemCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing problem categories.
 * <p>
 * This service handles the creation, retrieval, and validation of problem categories.
 * It provides functionality for adding new categories, retrieving categories by server,
 * and validating category data.
 * </p>
 * <p>
 * The service enforces authorization rules to ensure that only authorized members
 * can perform operations on problem categories.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemCategoryService {
    // Service modules
    private final AuthorizationService authorizationService;

    private final ProblemCategoryMapper problemCategoryMapper;

    private final GetProblemCategoryResponseDtoMapper getProblemCategoryResponseDtoMapper;
    private final CreateProblemCategoryRequestDtoMapper createProblemCategoryRequestDtoMapper;

    private static final String CREATE_PROBLEM_CATEGORY = "Problem Category Creation";
    private static final String GET_PROBLEM_CATEGORIES = "Problem Category Retrieval";

    /**
     * Retrieves all problem categories for a server with caching.
     * <p>
     * This method first validates that the requesting member has permission to view problem categories
     * in the specified channel, then retrieves all problem categories associated with the server.
     * Results are cached to improve performance for frequently accessed static data.
     * </p>
     *
     * @param getProblemCategoryRequestDto DTO containing member ID and channel ID
     * @return List of problem category response DTOs
     * @throws ValidationException If the member lacks proper authorization
     */
    @Cacheable(value = CacheConfiguration.PROBLEM_CATEGORIES_CACHE, 
               key = "#getProblemCategoryRequestDto.channelId")
    public List<GetProblemCategoryResponseDto> getServerProblemCategories(GetProblemCategoryRequestDto getProblemCategoryRequestDto) {
        // Check member authorization for viewing problem categories in this channel
        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                getProblemCategoryRequestDto.getMemberId(),
                getProblemCategoryRequestDto.getChannelId(),
                AuthorizationConstants.VIEW_PROBLEM_CATEGORY,
                GET_PROBLEM_CATEGORIES
        );

        log.debug("Retrieving problem categories from database for serverId to return dto: {}", authorizationContext.server().getId());

        // Retrieve all categories for the server and convert to DTOs
        List<ProblemCategory> problemCategories = problemCategoryMapper.selectByServerId(authorizationContext.server().getId());
        List<GetProblemCategoryResponseDto> getProblemCategoryResponseDtos = new ArrayList<>();
        for (ProblemCategory problemCategory : problemCategories) {
            getProblemCategoryResponseDtos.add(getProblemCategoryResponseDtoMapper.toProblemCategoryDto(problemCategory));
        }

        return getProblemCategoryResponseDtos;
    }

    /**
     * Retrieves problem categories by server ID with caching.
     * <p>
     * This method provides direct access to problem categories by server ID
     * with caching support for improved performance.
     * </p>
     *
     * @param serverId the server ID
     * @return List of problem categories for the server
     */
    @Cacheable(value = CacheConfiguration.PROBLEM_CATEGORIES_CACHE, 
               key = "#serverId")
    public List<ProblemCategory> getProblemCategoriesByServerId(Long serverId) {
        log.debug("Retrieving problem categories from database for serverId: {}", serverId);
        return problemCategoryMapper.selectByServerId(serverId);
    }

    /**
     * Adds a new problem category to the system and evicts related cache.
     * <p>
     * This method creates a new problem category in the database after validating
     * the input data and ensuring that the requesting member has proper authorization
     * to manage problem categories in the specified channel. Cache is evicted to
     * ensure consistency.
     * </p>
     *
     * @param createProblemCategoryRequestDto DTO containing the category data to be created
     * @throws ValidationException If the input data is invalid or the member lacks proper authorization
     */
    @CacheEvict(value = CacheConfiguration.PROBLEM_CATEGORIES_CACHE, 
                allEntries = true)
    public void addProblemCategory(CreateProblemCategoryRequestDto createProblemCategoryRequestDto) {
        // Check if member has permission to manage problem categories
        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                createProblemCategoryRequestDto.getMemberId(),
                createProblemCategoryRequestDto.getChannelId(),
                AuthorizationConstants.MANAGE_PROBLEM_CATEGORY,
                CREATE_PROBLEM_CATEGORY
        );

        // Convert DTO to entity and set server relationship
        ProblemCategory problemCategory = createProblemCategoryRequestDtoMapper.toProblemCategory(createProblemCategoryRequestDto);
        problemCategory.setServerId(authorizationContext.server().getId());

        // Validate category data before insertion
        validateNewProblemCategory(problemCategory);

        // Persist the new category
        problemCategoryMapper.insert(problemCategory);
        
        log.debug("Added new problem category and evicted cache for serverId: {}", authorizationContext.server().getId());
    }


    /**
     * Validates that a problem category exists for a given operation with caching.
     * <p>
     * This method checks if the provided problem category ID is valid and refers to an existing category.
     * It's used during operations that require a valid problem category reference.
     * Results are cached to improve performance for frequently validated categories.
     * </p>
     *
     * @param problemCategoryId The ID of the problem category to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return The validated ProblemCategory entity
     * @throws ValidationException If the category ID is null or refers to a non-existent category
     * @throws IllegalArgumentException If the operation parameter is null
     */
    @Cacheable(value = CacheConfiguration.PROBLEM_CATEGORIES_CACHE, 
               key = "'category:' + #problemCategoryId")
    public ProblemCategory validateProblemCategory(Long problemCategoryId, String operation) {
        // Check for required parameters
        if (problemCategoryId == null) {
            throw new ValidationException(operation, "problemCategoryId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }

        log.debug("Validating problem category from database for problemCategoryId: {}", problemCategoryId);

        // Verify the category exists in the database
        ProblemCategory problemCategory = problemCategoryMapper.selectById(problemCategoryId);
        if (problemCategory == null) {
            throw new ValidationException(operation, "problemCategoryId", "must refer to an existing problem category");
        }
        log.info("Validated problemCategoryId: {}", problemCategoryId);
        return problemCategory;
    }

    /**
     * Evicts problem categories cache for a specific server.
     * <p>
     * This method removes cached problem category data for a server,
     * typically called when category data is modified.
     * </p>
     *
     * @param serverId the server ID to evict cache for
     */
    @CacheEvict(value = CacheConfiguration.PROBLEM_CATEGORIES_CACHE, 
                key = "#serverId")
    public void evictProblemCategoriesCache(Long serverId) {
        log.debug("Evicting problem categories cache for serverId: {}", serverId);
    }

    /**
     * Evicts all problem categories cache entries.
     * <p>
     * This method removes all cached problem category data,
     * typically called during bulk operations or system maintenance.
     * </p>
     */
    @CacheEvict(value = CacheConfiguration.PROBLEM_CATEGORIES_CACHE, 
                allEntries = true)
    public void evictAllProblemCategoriesCache() {
        log.debug("Evicting all problem categories cache entries");
    }

    /**
     * Validates a new problem category before insertion.
     * <p>
     * This method ensures that the category name and description are not null or empty.
     * </p>
     *
     * @param problemCategory The problem category entity to validate
     * @throws ValidationException If the category name or description is null or empty
     */
    private void validateNewProblemCategory(ProblemCategory problemCategory) {
        String problemCategoryName = problemCategory.getCategoryName();
        String problemCategoryDescription = problemCategory.getDescription();

        // Ensure category name is provided
        if (problemCategoryName == null || problemCategoryName.trim().isEmpty()) {
            throw new ValidationException(CREATE_PROBLEM_CATEGORY, "problemCategoryName", "must not be empty");
        }

        // Ensure category description is provided
        if (problemCategoryDescription == null || problemCategoryDescription.trim().isEmpty()) {
            throw new ValidationException(CREATE_PROBLEM_CATEGORY, "problemCategoryDescription", "must not be empty");
        }
        log.info("Validated new problemCategory: {}", problemCategoryName);
    }
}
