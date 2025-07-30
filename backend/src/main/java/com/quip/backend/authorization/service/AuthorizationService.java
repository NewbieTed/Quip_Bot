package com.quip.backend.authorization.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.mapper.database.MemberChannelAuthorizationMapper;
import com.quip.backend.authorization.mapper.database.AuthorizationTypeMapper;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.authorization.model.AuthorizationType;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.model.Member;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.server.model.Server;
import com.quip.backend.server.service.ServerService;
import com.quip.backend.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for handling authorization-related operations.
 * <p>
 * This service provides functionality to validate user permissions and authorizations
 * for various operations within the application. It works with member, channel, and server
 * services to ensure proper access control.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final MemberService memberService;
    private final ChannelService channelService;
    private final ServerService serverService;

    private final AuthorizationTypeMapper authorizationTypeMapper;
    private final MemberChannelAuthorizationMapper memberChannelAuthorizationMapper;


    /**
     * Validates if a member has the required authorization for a specific operation on a channel.
     *
     * @param memberId The ID of the member whose authorization is being checked
     * @param channelId The ID of the channel on which the operation is being performed
     * @param authorizationType The type of authorization required (e.g., READ_PERMISSION)
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @return An AuthorizationContext containing the validated member, channel, server, and authorization
     * @throws IllegalArgumentException If any of the required parameters are null
     * @throws ValidationException If the member does not have the required authorization
     * @throws EntityNotFoundException If the authorization type does not exist
     */
    public AuthorizationContext validateAuthorization(Long memberId, Long channelId, String authorizationType, String operation) {
        // Validate all required parameters are present
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        if (channelId == null) {
            throw new IllegalArgumentException("channelId must not be null");
        }
        if (authorizationType == null) {
            throw new IllegalArgumentException("authorizationType must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        
        // Verify member, channel, and server all exist
        Member member = memberService.validateMember(memberId, operation);
        Channel channel = channelService.validateChannel(channelId, operation);
        Server server = serverService.assertServerExist(channel.getServerId());

        // Check if member has the specific permission for this channel
        long authorizationTypeId = this.getPermissionTypeId(authorizationType);
        MemberChannelAuthorization memberChannelAuthorization = memberChannelAuthorizationMapper.selectOne(
                new QueryWrapper<MemberChannelAuthorization>()
                        .eq("member_id", memberId)
                        .eq("channel_id", channelId)
                        .eq("authorization_type_id", authorizationTypeId));
        if (memberChannelAuthorization == null) {
            throw new ValidationException(operation, "member", "does not have authorization to " + authorizationType.toLowerCase());
        }

        // Return context with all validated entities
        return new AuthorizationContext(member, channel, server, memberChannelAuthorization);
    }

    /**
     * Retrieves the ID of a permission type by its name.
     *
     * @param authorizationTypeName The name of the authorization type to look up
     * @return The ID of the authorization type
     * @throws EntityNotFoundException If no authorization type with the given name exists
     */
    private long getPermissionTypeId(String authorizationTypeName) {
        // Look up the authorization type by name
        AuthorizationType authorizationType = authorizationTypeMapper.selectOne(new QueryWrapper<AuthorizationType>()
                .eq("authorization_type_name", authorizationTypeName));
        if (authorizationType == null) {
            // Throw system-level exception if the authorization type doesn't exist
            throw new EntityNotFoundException("AuthorizationType not found for name: " + authorizationTypeName);
        }
        return authorizationType.getId();
    }
}
