package com.quip.backend.authorization.service;

import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.mapper.database.AuthorizationTypeMapper;
import com.quip.backend.authorization.mapper.database.MemberChannelAuthorizationMapper;
import com.quip.backend.authorization.model.AuthorizationType;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.model.Member;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.server.model.Server;
import com.quip.backend.server.service.ServerService;
import com.quip.backend.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

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
    private MemberService memberService;

    @Mock
    private ChannelService channelService;

    @Mock
    private ServerService serverService;

    @Mock
    private AuthorizationTypeMapper authorizationTypeMapper;

    @Mock
    private MemberChannelAuthorizationMapper memberChannelAuthorizationMapper;

    // Test data constants
    private static final Long VALID_MEMBER_ID = 1L;
    private static final Long VALID_CHANNEL_ID = 10L;
    private static final Long VALID_SERVER_ID = 100L;
    private static final Long VALID_AUTH_TYPE_ID = 100L;
    private static final String VALID_AUTH_TYPE_NAME = "READ_PERMISSION";
    private static final String VALID_OPERATION = "READ_MESSAGE";


    private AuthorizationType validAuthorizationType;
    private MemberChannelAuthorization validMemberChannelAuthorization;
    private Server validServer;
    private Channel validChannel;
    private Member validMember;

    @BeforeEach
    void setUp() {
        reset(authorizationTypeMapper, memberChannelAuthorizationMapper);

        validAuthorizationType = createAuthorizationType(VALID_AUTH_TYPE_ID, VALID_AUTH_TYPE_NAME);
        validMemberChannelAuthorization = createMemberChannelAuthorization();
        validServer = createServer(VALID_SERVER_ID);
        validChannel = createChannel(VALID_CHANNEL_ID, VALID_SERVER_ID);
        validMember = createMember(VALID_MEMBER_ID);
    }


    @Nested
    @DisplayName("validateAuthorization() Tests")
    class ValidateAuthorizationTests {

        @Test
        @DisplayName("Should pass validation when member has required permission")
        void shouldPassValidation_WhenMemberHasRequiredPermission() {
            // Given
            mockValidAuthorizationDependencies();

            when(authorizationTypeMapper.selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME))
                    .thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID))
                    .thenReturn(validMemberChannelAuthorization);

            // When & Then
            AuthorizationContext authorizationContext = assertDoesNotThrow(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    VALID_AUTH_TYPE_NAME,
                    VALID_OPERATION
                )
            );

            assertNotNull(authorizationContext);
            assertEquals(validMember, authorizationContext.member());
            assertEquals(validChannel, authorizationContext.channel());
            assertEquals(validServer, authorizationContext.server());
            assertEquals(validMemberChannelAuthorization, authorizationContext.memberChannelAuthorization());

            verify(memberService).validateMember(VALID_MEMBER_ID, VALID_OPERATION);
            verify(channelService).validateChannel(VALID_CHANNEL_ID, VALID_OPERATION);
            verify(serverService).assertServerExist(VALID_SERVER_ID);
            verify(authorizationTypeMapper).selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME);
            verify(memberChannelAuthorizationMapper).selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID);
        }

        @Test
        @DisplayName("Should throw ValidationException when member lacks required permission")
        void shouldThrowValidationException_WhenMemberLacksRequiredPermission() {
            // Given
            mockValidAuthorizationDependencies();

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
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'member' does not have authorization to " + VALID_AUTH_TYPE_NAME.toLowerCase() + ".");

            verify(memberService).validateMember(VALID_MEMBER_ID, VALID_OPERATION);
            verify(channelService).validateChannel(VALID_CHANNEL_ID, VALID_OPERATION);
            verify(serverService).assertServerExist(VALID_SERVER_ID);
            verify(authorizationTypeMapper).selectByAuthorizationTypeName(VALID_AUTH_TYPE_NAME);
            verify(memberChannelAuthorizationMapper).selectByIds(VALID_MEMBER_ID, VALID_CHANNEL_ID, VALID_AUTH_TYPE_ID);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when authorization type does not exist during validation")
        void shouldThrowEntityNotFoundException_WhenAuthorizationTypeDoesNotExistDuringValidation() {
            // Given
            mockValidAuthorizationDependencies();

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

            verify(memberService).validateMember(VALID_MEMBER_ID, VALID_OPERATION);
            verify(channelService).validateChannel(VALID_CHANNEL_ID, VALID_OPERATION);
            verify(serverService).assertServerExist(VALID_SERVER_ID);
            verify(authorizationTypeMapper).selectByAuthorizationTypeName(nonExistentTypeName);
            verify(memberChannelAuthorizationMapper, never()).selectByIds(anyLong(), anyLong(), anyLong());
        }


        @Test
        @DisplayName("Should handle null parameters with IllegalArgumentException")
        void shouldHandleNullParametersWithIllegalArgumentException() {
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
    private AuthorizationType createAuthorizationType(Long authorizationTypeId, String authorizationTypeName) {
        AuthorizationType authorizationType = new AuthorizationType();
        authorizationType.setId(authorizationTypeId);
        authorizationType.setAuthorizationTypeName(authorizationTypeName);
        return authorizationType;
    }

    private MemberChannelAuthorization createMemberChannelAuthorization() {
        return new MemberChannelAuthorization();
    }

    private Server createServer(Long serverId) {
        Server server = new Server();
        server.setId(serverId);
        return server;
    }

    private Channel createChannel(Long channelId, Long serverId) {
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setServerId(serverId);
        return channel;
    }

    private Member createMember(Long memberId) {
        Member member = new Member();
        member.setId(memberId);
        return member;
    }

    private void mockValidAuthorizationDependencies() {
        when(memberService.validateMember(VALID_MEMBER_ID, VALID_OPERATION)).thenReturn(validMember);
        when(channelService.validateChannel(VALID_CHANNEL_ID, VALID_OPERATION)).thenReturn(validChannel);
        when(serverService.assertServerExist(VALID_SERVER_ID)).thenReturn(validServer);
    }
}
