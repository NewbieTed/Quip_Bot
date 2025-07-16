package com.quip.backend.authorization.service;

import com.quip.backend.authorization.mapper.database.AuthorizationTypeMapper;
import com.quip.backend.authorization.mapper.database.MemberChannelAuthorizationMapper;
import com.quip.backend.authorization.model.AuthorizationType;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthorizationService}.
 * <p>
 * This test class validates the authorization service functionality including
 * permission type retrieval and authorization validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Tests")
class AuthorizationServiceTest extends BaseTest {

    @InjectMocks
    private AuthorizationService authorizationService;

    @Mock
    private AuthorizationTypeMapper authorizationTypeMapper;

    @Mock
    private MemberChannelAuthorizationMapper memberChannelAuthorizationMapper;

    // Test data constants
    private static final Long VALID_MEMBER_ID = 1L;
    private static final Long VALID_CHANNEL_ID = 100L;
    private static final Long VALID_AUTH_TYPE_ID = 10L;
    private static final String VALID_AUTH_TYPE_NAME = "READ_PERMISSION";
    private static final String VALID_OPERATION = "READ_MESSAGE";
    
    private AuthorizationType validAuthorizationType;
    private MemberChannelAuthorization validMemberChannelAuthorization;

    @BeforeEach
    void setUp() {
        reset(authorizationTypeMapper, memberChannelAuthorizationMapper);

        validAuthorizationType = createAuthorizationType(VALID_AUTH_TYPE_ID, VALID_AUTH_TYPE_NAME);
        validMemberChannelAuthorization = createMemberChannelAuthorization();
    }


    @Nested
    @DisplayName("validateAuthorization() Tests")
    class ValidateAuthorizationTests {

        @Test
        @DisplayName("Should pass validation when member has required permission")
        void shouldPassValidation_WhenMemberHasRequiredPermission() {
            // Given
            when(authorizationTypeMapper.selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME))
                    .thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID))
                    .thenReturn(validMemberChannelAuthorization);

            // When & Then
            assertDoesNotThrow(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    VALID_AUTH_TYPE_NAME,
                    VALID_OPERATION
                )
            );

            verify(authorizationTypeMapper).selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME);
            verify(memberChannelAuthorizationMapper).selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID);
        }

        @Test
        @DisplayName("Should throw ValidationException when member lacks required permission")
        void shouldThrowValidationException_WhenMemberLacksRequiredPermission() {
            // Given
            when(authorizationTypeMapper.selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME))
                    .thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    VALID_AUTH_TYPE_NAME,
                    VALID_OPERATION
                )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("member")
            .hasMessageContaining("must have authorization to " + VALID_OPERATION.toLowerCase());

            verify(authorizationTypeMapper).selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME);
            verify(memberChannelAuthorizationMapper).selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when authorization type does not exist during validation")
        void shouldThrowEntityNotFoundException_WhenAuthorizationTypeDoesNotExistDuringValidation() {
            // Given
            String nonExistentTypeName = "NON_EXISTENT_TYPE";
            when(authorizationTypeMapper.selectByAuthorizationTypeName(nonExistentTypeName))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    nonExistentTypeName,
                    VALID_OPERATION
                )
            )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("AuthorizationType not found for name: " + nonExistentTypeName);

            verify(authorizationTypeMapper).selectByAuthorizationTypeName(nonExistentTypeName);
            verify(memberChannelAuthorizationMapper, never()).selectByIds(anyLong(), anyLong(), anyLong());
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -100L})
        @DisplayName("Should handle invalid member IDs")
        void shouldHandleInvalidMemberIds(Long invalidMemberId) {
            // Given
            when(authorizationTypeMapper.selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME))
                    .thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectByIds(invalidMemberId, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID))
                    .thenReturn(null);

            // When & Then
            assertValidationFails(authorizationService, invalidMemberId, VALID_CHANNEL_ID, VALID_AUTH_TYPE_NAME, VALID_OPERATION);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -100L})
        @DisplayName("Should handle invalid channel IDs")
        void shouldHandleInvalidChannelIds(Long invalidChannelId) {
            // Given
            when(authorizationTypeMapper.selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME))
                    .thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectByIds(VALID_MEMBER_ID, invalidChannelId, VALID_AUTH_TYPE_ID))
                    .thenReturn(null);

            // When & Then
            assertValidationFails(authorizationService, VALID_MEMBER_ID, invalidChannelId, VALID_AUTH_TYPE_NAME, VALID_OPERATION);
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // Null memberId
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    null,
                    VALID_CHANNEL_ID,
                    VALID_AUTH_TYPE_NAME,
                    VALID_OPERATION
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("memberId must not be null");

            // Null channelId
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    null,
                    VALID_AUTH_TYPE_NAME,
                    VALID_OPERATION
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("channelId must not be null");

            // Null authorizationType
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    null,
                    VALID_OPERATION
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("authorizationType must not be null");

            // Null operation
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    VALID_AUTH_TYPE_NAME,
                    null
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("operation must not be null");
        }
    }

    // Test data factory methods
    private AuthorizationType createAuthorizationType(Long id, String name) {
        AuthorizationType authorizationType = new AuthorizationType();
        authorizationType.setId(id);
        authorizationType.setAuthorizationTypeName(name);
        return authorizationType;
    }

    private MemberChannelAuthorization createMemberChannelAuthorization() {
        return new MemberChannelAuthorization();
    }

    private static void assertValidationFails(AuthorizationService service, Long memberId, Long channelId, String authType, String operation) {
        assertThatThrownBy(() -> service.validateAuthorization(memberId, channelId, authType, operation))
                .isInstanceOf(ValidationException.class);
    }
}
