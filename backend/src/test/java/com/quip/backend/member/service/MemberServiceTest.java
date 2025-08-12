package com.quip.backend.member.service;

import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.mapper.database.MemberMapper;
import com.quip.backend.member.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.in;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MemberService}.
 * <p>
 * This test class validates the member service functionality including
 * member validation and existence checking.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Tests")
public class MemberServiceTest extends BaseTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberMapper memberMapper;

    private static final Long VALID_MEMBER_ID = 1L;
    private static final String VALID_OPERATION = "MANAGE_MEMBER";

    private Member validMember;

    @BeforeEach
    void setUp() {
        reset(memberMapper);

        validMember = createMember();
    }


    /**
     * Tests for the validateMember method which ensures a member exists and is valid.
     * This nested class contains tests that verify member validation logic works correctly
     * for both valid and invalid member IDs.
     */
    @Nested
    @DisplayName("validateMember() Tests")
    class ValidateMemberTests {
        @Test
        @DisplayName("Should pass validation when member exists")
        void shouldPassValidation_WhenMemberExists() {
            // Given
            when(memberMapper.selectById(VALID_MEMBER_ID)).thenReturn(validMember);

            // When & Then
            Member member = assertDoesNotThrow(() ->
                    memberService.validateMember(
                            VALID_MEMBER_ID,
                            VALID_OPERATION
                    )
            );
            assertNotNull(member);
            assertEquals(validMember, member);

            verify(memberMapper).selectById(VALID_MEMBER_ID);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 10L, -1L, -100L})
        @DisplayName("Should throw ValidationException when member does not exist")
        void shouldThrowValidationException_WhenMemberDoesNotExist(Long invalidMemberId) {
            // Given
            when(memberMapper.selectById(invalidMemberId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    memberService.validateMember(
                            invalidMemberId,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'memberId' must refer to an existing member.");

            verify(memberMapper).selectById(invalidMemberId);
        }

        @Test
        @DisplayName("Should throw ValidationException when memberId is null")
        void shouldThrowValidationException_WhenMemberIdIsNull() {
            assertThatThrownBy(() ->
                    memberService.validateMember(
                            null,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'memberId' must not be null.");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when operation is null")
        void shouldThrowIllegalArgumentException_WhenOperationIsNull() {
            assertThatThrownBy(() ->
                    memberService.validateMember(
                            VALID_MEMBER_ID,
                            null
                    )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Parameter 'operation' must not be null.");
        }
    }


    /**
     * Tests for the getMemberById method which retrieves a member by ID.
     * This nested class contains tests that verify the method returns the correct member
     * and handles various scenarios appropriately.
     */
    @Nested
    @DisplayName("getMemberById() Tests")
    class GetMemberByIdTests {
        
        @Test
        @DisplayName("Should return member when member exists")
        void shouldReturnMember_WhenMemberExists() {
            // Given
            when(memberMapper.selectById(VALID_MEMBER_ID)).thenReturn(validMember);

            // When
            Member result = memberService.getMemberById(VALID_MEMBER_ID);

            // Then
            assertNotNull(result);
            assertEquals(validMember, result);
            verify(memberMapper).selectById(VALID_MEMBER_ID);
        }

        @Test
        @DisplayName("Should return null when member does not exist")
        void shouldReturnNull_WhenMemberDoesNotExist() {
            // Given
            Long nonExistentMemberId = 999L;
            when(memberMapper.selectById(nonExistentMemberId)).thenReturn(null);

            // When
            Member result = memberService.getMemberById(nonExistentMemberId);

            // Then
            assertNull(result);
            verify(memberMapper).selectById(nonExistentMemberId);
        }

        @Test
        @DisplayName("Should handle null memberId parameter")
        void shouldHandleNullMemberId() {
            // Given
            when(memberMapper.selectById(null)).thenReturn(null);

            // When
            Member result = memberService.getMemberById(null);

            // Then
            assertNull(result);
            verify(memberMapper).selectById(null);
        }

        @ParameterizedTest
        @ValueSource(longs = {1L, 2L, 100L, 999L})
        @DisplayName("Should call mapper with correct memberId")
        void shouldCallMapperWithCorrectMemberId(Long memberId) {
            // Given
            Member expectedMember = createMember();
            when(memberMapper.selectById(memberId)).thenReturn(expectedMember);

            // When
            Member result = memberService.getMemberById(memberId);

            // Then
            assertEquals(expectedMember, result);
            verify(memberMapper).selectById(memberId);
        }
    }

    /**
     * Creates a simple Member entity for testing.
     * 
     * @return A basic Member instance for test cases
     */
    private Member createMember() {
        return new Member();
    }
}
