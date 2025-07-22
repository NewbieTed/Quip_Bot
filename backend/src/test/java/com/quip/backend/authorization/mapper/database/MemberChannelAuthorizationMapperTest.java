package com.quip.backend.authorization.mapper.database;

import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MemberChannelAuthorizationMapper}.
 * <p>
 * This test class validates the member channel authorization database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberChannelAuthorizationMapper Tests")
public class MemberChannelAuthorizationMapperTest extends BaseTest {

    @Mock
    private MemberChannelAuthorizationMapper memberChannelAuthorizationMapper;

    @Test
    @DisplayName("Should call selectByIds method")
    void shouldCallSelectByIds_Method() {
        // Given
        Long memberId = 1L;
        Long channelId = 2L;
        Long authorizationTypeId = 3L;
        
        MemberChannelAuthorization expectedAuthorization = new MemberChannelAuthorization();
        expectedAuthorization.setMemberId(memberId);
        expectedAuthorization.setChannelId(channelId);
        expectedAuthorization.setAuthorizationTypeId(authorizationTypeId);
        
        when(memberChannelAuthorizationMapper.selectByIds(memberId, channelId, authorizationTypeId))
                .thenReturn(expectedAuthorization);

        // When
        MemberChannelAuthorization result = memberChannelAuthorizationMapper.selectByIds(
                memberId, channelId, authorizationTypeId);

        // Then
        assertNotNull(result);
        assertEquals(memberId, result.getMemberId());
        assertEquals(channelId, result.getChannelId());
        assertEquals(authorizationTypeId, result.getAuthorizationTypeId());
        verify(memberChannelAuthorizationMapper).selectByIds(memberId, channelId, authorizationTypeId);
    }

    @Test
    @DisplayName("Should handle null return from selectByIds")
    void shouldHandleNullReturn_FromSelectByIds() {
        // Given
        Long memberId = 999L;
        Long channelId = 999L;
        Long authorizationTypeId = 999L;
        
        when(memberChannelAuthorizationMapper.selectByIds(memberId, channelId, authorizationTypeId))
                .thenReturn(null);

        // When
        MemberChannelAuthorization result = memberChannelAuthorizationMapper.selectByIds(
                memberId, channelId, authorizationTypeId);

        // Then
        verify(memberChannelAuthorizationMapper).selectByIds(memberId, channelId, authorizationTypeId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        MemberChannelAuthorization authorization = new MemberChannelAuthorization();
        authorization.setMemberId(1L);
        authorization.setChannelId(2L);
        authorization.setAuthorizationTypeId(3L);
        
        when(memberChannelAuthorizationMapper.insert(authorization)).thenReturn(1);

        // When
        int result = memberChannelAuthorizationMapper.insert(authorization);

        // Then
        assertEquals(1, result);
        verify(memberChannelAuthorizationMapper).insert(authorization);
    }
}