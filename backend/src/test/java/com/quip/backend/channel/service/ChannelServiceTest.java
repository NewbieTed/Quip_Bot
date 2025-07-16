package com.quip.backend.channel.service;

import com.quip.backend.channel.mapper.database.ChannelMapper;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelService Tests")
public class ChannelServiceTest extends BaseTest {

    @InjectMocks
    private ChannelService channelService;

    @Mock
    private ChannelMapper channelMapper;

    // Test data constants
    private static final Long VALID_CHANNEL_ID = 1L;
    private static final String VALID_OPERATION = "MANAGE_CHANNEL";

    private Channel validChannel;

    @BeforeEach
    void setUp() {
        reset(channelMapper);

        validChannel = createChannel();
    }

    @Nested
    @DisplayName("validateChannel() Tests")
    class ValidateChannelTests {
        @Test
        @DisplayName("Should pass validation when channel exists")
        void shouldPassValidation_WhenChannelExists() {
            // Given
            when(channelMapper.selectById(VALID_CHANNEL_ID)).thenReturn(validChannel);

            // When & Then
            Channel channel = assertDoesNotThrow(() ->
                channelService.validateChannel(
                        VALID_CHANNEL_ID,
                        VALID_OPERATION
                )
            );
            assertNotNull(channel);
            assertEquals(validChannel, channel);

            verify(channelMapper).selectById(VALID_CHANNEL_ID);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 10L, -1L, -100L})
        @DisplayName("Should throw ValidationException when channel does not exist")
        void shouldThrowValidationException_WhenChannelDoesNotExist(Long invalidChannelId) {
            // Given
            when(channelMapper.selectById(invalidChannelId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    channelService.validateChannel(
                            invalidChannelId,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'channelId' must refer to an existing channel.");

            verify(channelMapper).selectById(invalidChannelId);
        }

        @Test
        @DisplayName("Should throw ValidationException when channel ID is null")
        void shouldThrowValidationException_WhenChannelIdIsNull() {
            assertThatThrownBy(() ->
                    channelService.validateChannel(
                            null,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'channelId' must not be null.");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when operation is null")
        void shouldThrowIllegalArgumentException_WhenOperationIsNull() {
            assertThatThrownBy(() ->
                    channelService.validateChannel(
                            VALID_CHANNEL_ID,
                            null
                    )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Parameter 'operation' must not be null.");
        }
    }


    private Channel createChannel() {
        return new Channel();
    }
}
