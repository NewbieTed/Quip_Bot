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

@ExtendWith(MockitoExtension.class)
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


    private Member createMember() {
        return new Member();
    }
}
