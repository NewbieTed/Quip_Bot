
package com.quip.backend.server.service;

import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import jakarta.persistence.EntityNotFoundException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerServiceTest extends BaseTest {

    @InjectMocks
    private ServerService serverService;

    @Mock
    private ServerMapper serverMapper;

    private static final Long VALID_SERVER_ID = 1L;
    private static final String VALID_OPERATION = "MANAGE_SERVER";

    private Server validServer;

    @BeforeEach
    void setUp() {
        reset(serverMapper);

        validServer = createServer();
    }


    @Nested
    @DisplayName("validateServer() Tests")
    class ValidateServerTests {
        @Test
        @DisplayName("Should pass validation when server exists")
        void shouldPassValidation_WhenServerExists() {
            // Given
            when(serverMapper.selectById(VALID_SERVER_ID)).thenReturn(validServer);

            // When & Then
            Server server = assertDoesNotThrow(() ->
                    serverService.validateServer(
                            VALID_SERVER_ID,
                            VALID_OPERATION
                    )
            );
            assertNotNull(server);
            assertEquals(validServer, server);

            verify(serverMapper).selectById(VALID_SERVER_ID);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 10L, -1L, -100L})
        @DisplayName("Should throw ValidationException when server does not exist")
        void shouldThrowValidationException_WhenServerDoesNotExist(Long invalidServerId) {
            // Given
            when(serverMapper.selectById(invalidServerId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    serverService.validateServer(
                            invalidServerId,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'serverId' must refer to an existing server.");

            verify(serverMapper).selectById(invalidServerId);
        }

        @Test
        @DisplayName("Should throw ValidationException when serverId is null")
        void shouldThrowValidationException_WhenServerIdIsNull() {
            assertThatThrownBy(() ->
                    serverService.validateServer(
                            null,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'serverId' must not be null.");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when operation is null")
        void shouldThrowIllegalArgumentException_WhenOperationIsNull() {
            assertThatThrownBy(() ->
                    serverService.validateServer(
                            VALID_SERVER_ID,
                            null
                    )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Parameter 'operation' must not be null.");
        }
    }



    @Nested
    @DisplayName("assertServerExist() Tests")
    class assertServerExistTests {
        @Test
        @DisplayName("Should pass validation when server exists")
        void shouldPassValidation_WhenServerExists() {
            // Given
            when(serverMapper.selectById(VALID_SERVER_ID)).thenReturn(validServer);

            // When
            Server result = assertDoesNotThrow(() ->
                    serverService.assertServerExist(
                            VALID_SERVER_ID
                    )
            );

            // Then
            assertNotNull(result);
            assertEquals(validServer, result);
            verify(serverMapper).selectById(VALID_SERVER_ID);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 10L, -1L, -100L})
        @DisplayName("Should throw EntityNotFoundException when server does not exist")
        void shouldThrowValidationException_WhenServerDoesNotExist(Long invalidServerId) {
            // Given
            when(serverMapper.selectById(invalidServerId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    serverService.assertServerExist(
                            invalidServerId
                    )
            )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Server not found for serverId: " + invalidServerId);

            verify(serverMapper).selectById(invalidServerId);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serverId is null")
        void shouldThrowValidationException_WhenServerIdIsNull() {
            assertThatThrownBy(() ->
                    serverService.assertServerExist(
                            null
                    )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Parameter 'serverId' must not be null");
        }
    }


    private Server createServer() {
        return new Server();
    }
}
