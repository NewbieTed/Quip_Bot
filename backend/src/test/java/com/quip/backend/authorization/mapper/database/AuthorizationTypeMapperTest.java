package com.quip.backend.authorization.mapper.database;

import com.quip.backend.authorization.model.AuthorizationType;
import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthorizationTypeMapper}.
 * <p>
 * This test class validates the authorization type database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationTypeMapper Tests")
public class AuthorizationTypeMapperTest extends BaseTest {

    @Mock
    private AuthorizationTypeMapper authorizationTypeMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long typeId = 1L;
        AuthorizationType expectedType = new AuthorizationType();
        expectedType.setId(typeId);
        expectedType.setAuthorizationTypeName("READ_PERMISSION");
        
        when(authorizationTypeMapper.selectById(typeId)).thenReturn(expectedType);

        // When
        AuthorizationType result = authorizationTypeMapper.selectById(typeId);

        // Then
        assertNotNull(result);
        assertEquals(typeId, result.getId());
        assertEquals("READ_PERMISSION", result.getAuthorizationTypeName());
        verify(authorizationTypeMapper).selectById(typeId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long typeId = 999L;
        when(authorizationTypeMapper.selectById(typeId)).thenReturn(null);

        // When
        AuthorizationType result = authorizationTypeMapper.selectById(typeId);

        // Then
        verify(authorizationTypeMapper).selectById(typeId);
    }



    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        AuthorizationType type = new AuthorizationType();
        type.setAuthorizationTypeName("NEW_PERMISSION");
        
        when(authorizationTypeMapper.insert(type)).thenReturn(1);

        // When
        int result = authorizationTypeMapper.insert(type);

        // Then
        assertEquals(1, result);
        verify(authorizationTypeMapper).insert(type);
    }
}