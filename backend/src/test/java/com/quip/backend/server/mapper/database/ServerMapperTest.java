package com.quip.backend.server.mapper.database;

import com.quip.backend.common.BaseTest;
import com.quip.backend.server.model.Server;
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
 * Unit tests for {@link ServerMapper}.
 * <p>
 * This test class validates the server database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServerMapper Tests")
public class ServerMapperTest extends BaseTest {

    @Mock
    private ServerMapper serverMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long serverId = 1L;
        Server expectedServer = new Server();
        expectedServer.setId(serverId);
        expectedServer.setServerName("Test Server");
        
        when(serverMapper.selectById(serverId)).thenReturn(expectedServer);

        // When
        Server result = serverMapper.selectById(serverId);

        // Then
        assertNotNull(result);
        assertEquals(serverId, result.getId());
        assertEquals("Test Server", result.getServerName());
        verify(serverMapper).selectById(serverId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long serverId = 999L;
        when(serverMapper.selectById(serverId)).thenReturn(null);

        // When
        Server result = serverMapper.selectById(serverId);

        // Then
        verify(serverMapper).selectById(serverId);
    }

    @Test
    @DisplayName("Should call selectByChannelId method")
    void shouldCallSelectByChannelId_Method() {
        // Given
        Long channelId = 1L;
        Server expectedServer = new Server();
        expectedServer.setId(10L);
        expectedServer.setServerName("Test Server");
        
        when(serverMapper.selectByChannelId(channelId)).thenReturn(expectedServer);

        // When
        Server result = serverMapper.selectByChannelId(channelId);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Test Server", result.getServerName());
        verify(serverMapper).selectByChannelId(channelId);
    }

    @Test
    @DisplayName("Should handle null return from selectByChannelId")
    void shouldHandleNullReturn_FromSelectByChannelId() {
        // Given
        Long channelId = 999L;
        when(serverMapper.selectByChannelId(channelId)).thenReturn(null);

        // When
        Server result = serverMapper.selectByChannelId(channelId);

        // Then
        verify(serverMapper).selectByChannelId(channelId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        Server server = new Server();
        server.setServerName("New Server");
        
        when(serverMapper.insert(server)).thenReturn(1);

        // When
        int result = serverMapper.insert(server);

        // Then
        assertEquals(1, result);
        verify(serverMapper).insert(server);
    }
}