package com.quip.backend.channel.service;

import com.quip.backend.channel.mapper.database.ChannelMapper;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for channel-related operations.
 * <p>
 * This service provides functionality to validate and manage channels within the application.
 * It ensures that channel references are valid and handles channel-related validation
 * for various operations throughout the system.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelMapper channelMapper;


    /**
     * Validates that a channel exists for a given operation.
     * <p>
     * This method checks if the provided channel ID is valid and refers to an existing channel.
     * It's used during operations that require a valid channel reference.
     * </p>
     *
     * @param channelId The ID of the channel to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return The validated Channel entity
     * @throws ValidationException If the channel ID is null or refers to a non-existent channel
     * @throws IllegalArgumentException If the operation parameter is null
     */
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
