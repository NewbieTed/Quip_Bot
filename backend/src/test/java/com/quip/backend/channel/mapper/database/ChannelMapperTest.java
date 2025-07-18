package com.quip.backend.channel.mapper.database;

import com.quip.backend.channel.model.Channel;
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
 * Unit tests for {@link ChannelMapper}.
 * <p>
 * This test class validates the channel database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelMapper Tests")
public class ChannelMapperTest extends BaseTest {

    @Mock
    private ChannelMapper channelMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long channelId = 1L;
        Channel expectedChannel = new Channel();
        expectedChannel.setId(channelId);
        expectedChannel.setChannelName("general");
        expectedChannel.setServerId(10L);
        
        when(channelMapper.selectById(channelId)).thenReturn(expectedChannel);

        // When
        Channel result = channelMapper.selectById(channelId);

        // Then
        assertNotNull(result);
        assertEquals(channelId, result.getId());
        assertEquals("general", result.getChannelName());
        assertEquals(10L, result.getServerId());
        verify(channelMapper).selectById(channelId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long channelId = 999L;
        when(channelMapper.selectById(channelId)).thenReturn(null);

        // When
        Channel result = channelMapper.selectById(channelId);

        // Then
        verify(channelMapper).selectById(channelId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        Channel channel = new Channel();
        channel.setChannelName("new-channel");
        channel.setServerId(10L);
        
        when(channelMapper.insert(channel)).thenReturn(1);

        // When
        int result = channelMapper.insert(channel);

        // Then
        assertEquals(1, result);
        verify(channelMapper).insert(channel);
    }
}