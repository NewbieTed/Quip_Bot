package com.quip.backend.authorization.service;

import com.quip.backend.authorization.mapper.database.MemberChannelAuthorizationMapper;
import com.quip.backend.authorization.mapper.database.AuthorizationTypeMapper;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.authorization.model.AuthorizationType;
import com.quip.backend.common.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthorizationTypeMapper authorizationTypeMapper;
    private final MemberChannelAuthorizationMapper memberChannelAuthorizationMapper;


    public void validateAuthorization(Long memberId, Long channelId, String authorizationType, String operation) {
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
        long authorizationTypeId = this.getPermissionTypeId(authorizationType);
        if (!this.hasPermission(memberId, channelId, authorizationTypeId)) {
            throw new ValidationException(operation, "member", "must have authorization to " + operation.toLowerCase());
        }
    }

    private boolean hasPermission(Long memberId, Long channelId, Long authorizationTypeId) {
        MemberChannelAuthorization memberChannelAuthorization = memberChannelAuthorizationMapper.selectByIds(memberId, channelId, authorizationTypeId);
        return memberChannelAuthorization != null;
    }

    private long getPermissionTypeId(String authorizationTypeName) {
        AuthorizationType authorizationType = authorizationTypeMapper.selectByAuthorizationTypeName(authorizationTypeName);
        if (authorizationType == null) {
            throw new EntityNotFoundException("AuthorizationType not found for name: " + authorizationTypeName);
        }
        return authorizationType.getId();
    }
}
