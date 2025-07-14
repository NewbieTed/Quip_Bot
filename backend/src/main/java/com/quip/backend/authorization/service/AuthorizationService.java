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


    public long getPermissionTypeId(String authorizationTypeName) {
        AuthorizationType authorizationType = authorizationTypeMapper.selectByAuthorizationTypeName(authorizationTypeName);
        if (authorizationType == null) {
            throw new EntityNotFoundException("AuthorizationType not found for name: " + authorizationTypeName);
        }
        return authorizationType.getId();
    }

    public void validateAuthorization(Long memberId, Long channelId, String authorizationType, String operation) {
        long authorizationTypeId = this.getPermissionTypeId(authorizationType);
        if (!this.hasPermission(memberId, channelId, authorizationTypeId)) {
            throw new ValidationException(operation, "member", "must have authorization to " + operation.toLowerCase());
        }
    }

    private boolean hasPermission(Long memberId, Long channelId, Long authorizationTypeId) {
        MemberChannelAuthorization memberChannelAuthorization = memberChannelAuthorizationMapper.selectByIds(memberId, channelId, authorizationTypeId);
        return memberChannelAuthorization != null;
    }
}
