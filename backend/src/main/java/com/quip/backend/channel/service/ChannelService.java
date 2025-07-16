package com.quip.backend.channel.service;

import com.quip.backend.channel.mapper.database.ChannelMapper;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelMapper channelMapper;

    public Long findServerId(Long channelId) {
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            return null;
        }
        return channel.getServerId();
    }


    public void validateChannel(Long channelId, String operation) {
        if (!this.isChannelExist(channelId)) {
            throw new ValidationException(operation, "channelId", "must refer to an existing channel");
        }
        log.info("Validated channelId: {}", channelId);
    }

    private boolean isChannelExist(Long channelId) {
        Channel channel = channelMapper.selectById(channelId);
        return channel != null;
    }

}
