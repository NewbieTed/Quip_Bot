package com.quip.backend.server.service;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link ServerRoleService}.
 * <p>
 * This test class validates the server role service functionality.
 * Currently, the service is empty but this test ensures proper instantiation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServerRoleService Tests")
public class ServerRoleServiceTest extends BaseTest {

    @InjectMocks
    private ServerRoleService serverRoleService;

    @Test
    @DisplayName("Should instantiate service successfully")
    void shouldInstantiateService_Successfully() {
        // When & Then
        assertNotNull(serverRoleService);
    }
}
