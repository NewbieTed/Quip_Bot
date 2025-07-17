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


    public Channel validateChannel(Long channelId, String operation) {
        if (channelId == null) {
            throw new ValidationException(operation, "channelId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            throw new ValidationException(operation, "channelId", "must refer to an existing channel");
        }
        log.info("Validated channelId: {}", channelId);
        return channel;
    }

}
