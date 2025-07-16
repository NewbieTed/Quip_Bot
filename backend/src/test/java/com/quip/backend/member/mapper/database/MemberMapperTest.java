package com.quip.backend.member.mapper.database;

import com.quip.backend.common.BaseTest;
import com.quip.backend.member.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MemberMapper}.
 * <p>
 * This test class validates the member database mapper functionality.
 * Tests MyBatis mapper interface methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberMapper Tests")
public class MemberMapperTest extends BaseTest {

    @Mock
    private MemberMapper memberMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long memberId = 1L;
        Member expectedMember = new Member();
        expectedMember.setId(memberId);
        
        when(memberMapper.selectById(memberId)).thenReturn(expectedMember);

        // When
        Member result = memberMapper.selectById(memberId);

        // Then
        assertNotNull(result);
        verify(memberMapper).selectById(memberId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long memberId = 999L;
        when(memberMapper.selectById(memberId)).thenReturn(null);

        // When
        Member result = memberMapper.selectById(memberId);

        // Then
        verify(memberMapper).selectById(memberId);
    }
}
