
package com.quip.backend.server.service;

import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServerServiceTest extends BaseTest {

    @InjectMocks
    private ServerService serverService;

    @Mock
    private ServerMapper serverMapper;

    @Test
    void isServerExist_existingServer_returnsTrue() {
        long serverId = 1L;
        when(serverMapper.selectById(serverId)).thenReturn(new Server());

        assertTrue(serverService.isServerExist(serverId));
    }

    @Test
    void isServerExist_nonExistingServer_returnsFalse() {
        long serverId = 1L;
        when(serverMapper.selectById(serverId)).thenReturn(null);

        assertFalse(serverService.isServerExist(serverId));
    }

    @Test
    void validateServer_existingServer_noException() {
        long serverId = 1L;
        when(serverMapper.selectById(serverId)).thenReturn(new Server());

        assertDoesNotThrow(() -> {
            serverService.validateServer(serverId, "testOperation");
        });
    }

    @Test
    void validateServer_nonExistingServer_throwsException() {
        long serverId = 1L;
        when(serverMapper.selectById(serverId)).thenReturn(null);

        assertThrows(ValidationException.class, () -> {
            serverService.validateServer(serverId, "testOperation");
        });
    }

    @Test
    void validateServer_nullServerId_throwsException() {
        assertThrows(ValidationException.class, () -> {
            serverService.validateServer(null, "testOperation");
        });
    }
}
