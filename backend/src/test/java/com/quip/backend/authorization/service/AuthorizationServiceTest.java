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
import static org.mockito.ArgumentMatchers.any;
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


    // Tests for validateAuthorization method removed as the underlying mapper methods have been refactored


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

    @Nested
    @DisplayName("validateAuthorization method tests")
    class ValidateAuthorizationTests {

        @Test
        @DisplayName("Should successfully validate authorization when all conditions are met")
        void shouldSuccessfullyValidateAuthorizationWhenAllConditionsMet() {
            // Given
            mockValidAuthorizationDependencies();
            when(authorizationTypeMapper.selectOne(any())).thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectOne(any())).thenReturn(validMemberChannelAuthorization);

            // When
            AuthorizationContext result = authorizationService.validateAuthorization(
                VALID_MEMBER_ID,
                VALID_CHANNEL_ID,
                VALID_AUTH_TYPE_NAME,
                VALID_OPERATION
            );

            // Then
            assertNotNull(result);
            assertEquals(validMember, result.member());
            assertEquals(validChannel, result.channel());
            assertEquals(validServer, result.server());
            assertEquals(validMemberChannelAuthorization, result.memberChannelAuthorization());
        }

        @Test
        @DisplayName("Should throw ValidationException when member lacks authorization")
        void shouldThrowValidationExceptionWhenMemberLacksAuthorization() {
            // Given
            mockValidAuthorizationDependencies();
            when(authorizationTypeMapper.selectOne(any())).thenReturn(validAuthorizationType);
            when(memberChannelAuthorizationMapper.selectOne(any())).thenReturn(null);

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
            .hasMessageContaining("does not have authorization to read_permission");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when authorization type does not exist")
        void shouldThrowEntityNotFoundExceptionWhenAuthorizationTypeDoesNotExist() {
            // Given
            mockValidAuthorizationDependencies();
            when(authorizationTypeMapper.selectOne(any())).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                authorizationService.validateAuthorization(
                    VALID_MEMBER_ID,
                    VALID_CHANNEL_ID,
                    VALID_AUTH_TYPE_NAME,
                    VALID_OPERATION
                )
            )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("AuthorizationType not found for name: " + VALID_AUTH_TYPE_NAME);
        }
    }

    /**
     * Helper methods for creating test data
     */
    
    /**
     * Creates an AuthorizationType entity with the specified ID and name.
     * Used for mocking authorization type data in tests.
     */
    private AuthorizationType createAuthorizationType(Long authorizationTypeId, String authorizationTypeName) {
        AuthorizationType authorizationType = new AuthorizationType();
        authorizationType.setId(authorizationTypeId);
        authorizationType.setAuthorizationTypeName(authorizationTypeName);
        return authorizationType;
    }

    /**
     * Creates a simple MemberChannelAuthorization entity.
     * Used for mocking authorization data in tests.
     */
    private MemberChannelAuthorization createMemberChannelAuthorization() {
        return new MemberChannelAuthorization();
    }

    /**
     * Creates a Server entity with the specified ID.
     * Used for mocking server data in tests.
     */
    private Server createServer(Long serverId) {
        Server server = new Server();
        server.setId(serverId);
        return server;
    }

    /**
     * Creates a Channel entity with the specified ID and server ID.
     * Used for mocking channel data in tests.
     */
    private Channel createChannel(Long channelId, Long serverId) {
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setServerId(serverId);
        return channel;
    }

    /**
     * Creates a Member entity with the specified ID.
     * Used for mocking member data in tests.
     */
    private Member createMember(Long memberId) {
        Member member = new Member();
        member.setId(memberId);
        return member;
    }

    /**
     * Sets up common mock behaviors for the dependent services.
     * This method configures the member, channel, and server services to return
     * valid test entities when called with the test constants.
     */
    private void mockValidAuthorizationDependencies() {
        when(memberService.validateMember(VALID_MEMBER_ID, VALID_OPERATION)).thenReturn(validMember);
        when(channelService.validateChannel(VALID_CHANNEL_ID, VALID_OPERATION)).thenReturn(validChannel);
        when(serverService.assertServerExist(VALID_SERVER_ID)).thenReturn(validServer);
    }
}
