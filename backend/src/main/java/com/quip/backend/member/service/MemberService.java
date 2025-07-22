package com.quip.backend.member.service;

import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.mapper.database.MemberMapper;
import com.quip.backend.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Validates that a member exists for a given operation.
     * <p>
     * This method checks if the provided member ID is valid and refers to an existing member.
     * It's used during operations that require a valid member reference.
     * </p>
     *
     * @param memberId The ID of the member to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return The validated Member entity
     * @throws ValidationException If the member ID is null or refers to a non-existent member
     * @throws IllegalArgumentException If the operation parameter is null
     */
    public Member validateMember(Long memberId, String operation) {
        if (memberId == null) {
            throw new ValidationException(operation, "memberId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new ValidationException(operation, "memberId", "must refer to an existing member");
        }
        log.info("Validated memberId: {}", memberId);
        return member;
    }
}
