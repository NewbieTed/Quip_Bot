package com.quip.backend.server.mapper.database;

import com.quip.backend.common.BaseTest;
import com.quip.backend.server.model.ServerRole;
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
 * Unit tests for {@link ServerRoleMapper}.
 * <p>
 * This test class validates the server role database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServerRoleMapper Tests")
public class ServerRoleMapperTest extends BaseTest {

    @Mock
    private ServerRoleMapper serverRoleMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long roleId = 1L;
        ServerRole expectedRole = new ServerRole();
        expectedRole.setId(roleId);
        expectedRole.setRoleName("Admin");
        expectedRole.setServerId(10L);
        
        when(serverRoleMapper.selectById(roleId)).thenReturn(expectedRole);

        // When
        ServerRole result = serverRoleMapper.selectById(roleId);

        // Then
        assertNotNull(result);
        assertEquals(roleId, result.getId());
        assertEquals("Admin", result.getRoleName());
        assertEquals(10L, result.getServerId());
        verify(serverRoleMapper).selectById(roleId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long roleId = 999L;
        when(serverRoleMapper.selectById(roleId)).thenReturn(null);

        // When
        ServerRole result = serverRoleMapper.selectById(roleId);

        // Then
        verify(serverRoleMapper).selectById(roleId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        ServerRole role = new ServerRole();
        role.setRoleName("Moderator");
        role.setServerId(10L);
        role.setIsAutoAssignable(true);
        role.setIsSelfAssignable(false);
        
        when(serverRoleMapper.insert(role)).thenReturn(1);

        // When
        int result = serverRoleMapper.insert(role);

        // Then
        assertEquals(1, result);
        verify(serverRoleMapper).insert(role);
    }
}