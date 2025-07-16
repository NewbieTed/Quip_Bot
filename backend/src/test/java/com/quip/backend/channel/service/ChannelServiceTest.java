
package com.quip.backend.channel.service;

import com.quip.backend.channel.mapper.database.ChannelMapper;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChannelServiceTest extends BaseTest {

    @InjectMocks
    private ChannelService channelService;

    @Mock
    private ChannelMapper channelMapper;

    @Test
    void findServerId_existingChannel_returnsServerId() {
        long channelId = 1L;
        long serverId = 100L;
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setServerId(serverId);

        when(channelMapper.selectById(channelId)).thenReturn(channel);

        Long resultServerId = channelService.findServerId(channelId);

        assertEquals(serverId, resultServerId);
    }

    @Test
    void findServerId_nonExistingChannel_returnsNull() {
        long channelId = 1L;

        when(channelMapper.selectById(channelId)).thenReturn(null);

        Long resultServerId = channelService.findServerId(channelId);

        assertNull(resultServerId);
    }

    @Test
    void validateChannel_existingChannel_noException() {
        long channelId = 1L;
        Channel channel = new Channel();
        channel.setId(channelId);

        when(channelMapper.selectById(channelId)).thenReturn(channel);

        assertDoesNotThrow(() -> {
            channelService.validateChannel(channelId, "testOperation");
        });
    }

    @Test
    void validateChannel_nonExistingChannel_throwsException() {
        long channelId = 1L;

        when(channelMapper.selectById(channelId)).thenReturn(null);

        assertThrows(ValidationException.class, () -> {
            channelService.validateChannel(channelId, "testOperation");
        });
    }
}
