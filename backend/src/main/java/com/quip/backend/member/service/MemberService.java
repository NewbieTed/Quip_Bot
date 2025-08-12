package com.quip.backend.member.service;

import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.member.mapper.database.MemberMapper;
import com.quip.backend.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for member-related operations.
 * <p>
 * This service provides functionality to validate and manage members within the application.
 * It ensures that member references are valid and handles member-related validation
 * for various operations throughout the system.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    /**
     * Validates that a member exists for a given operation with caching.
     * <p>
     * This method checks if the provided member ID is valid and refers to an existing member.
     * It's used during operations that require a valid member reference.
     * Results are cached to improve performance for frequently validated members.
     * </p>
     *
     * @param memberId The ID of the member to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return The validated Member entity
     * @throws ValidationException If the member ID is null or refers to a non-existent member
     * @throws IllegalArgumentException If the operation parameter is null
     */
    @Cacheable(value = CacheConfiguration.MEMBER_DATA_CACHE, 
               key = "#memberId")
    public Member validateMember(Long memberId, String operation) {
        if (memberId == null) {
            throw new ValidationException(operation, "memberId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        
        log.debug("Validating member from database for memberId: {}", memberId);
        
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new ValidationException(operation, "memberId", "must refer to an existing member");
        }
        log.info("Validated memberId: {}", memberId);
        return member;
    }

    /**
     * Retrieves a member by ID with caching.
     * <p>
     * This method provides direct access to member data by ID
     * with caching support for improved performance.
     * </p>
     *
     * @param memberId the member ID
     * @return the Member entity
     */
    @Cacheable(value = CacheConfiguration.MEMBER_DATA_CACHE, 
               key = "'member:' + #memberId")
    public Member getMemberById(Long memberId) {
        log.debug("Retrieving member from database for memberId: {}", memberId);
        return memberMapper.selectById(memberId);
    }

    /**
     * Evicts member cache for a specific member.
     * <p>
     * This method removes cached member data for a member,
     * typically called when member data is modified.
     * </p>
     *
     * @param memberId the member ID to evict cache for
     */
    @CacheEvict(value = CacheConfiguration.MEMBER_DATA_CACHE, 
                key = "#memberId")
    public void evictMemberCache(Long memberId) {
        log.debug("Evicting member cache for memberId: {}", memberId);
    }

    /**
     * Evicts all member cache entries.
     * <p>
     * This method removes all cached member data,
     * typically called during bulk operations or system maintenance.
     * </p>
     */
    @CacheEvict(value = CacheConfiguration.MEMBER_DATA_CACHE, 
                allEntries = true)
    public void evictAllMemberCache() {
        log.debug("Evicting all member cache entries");
    }
}
