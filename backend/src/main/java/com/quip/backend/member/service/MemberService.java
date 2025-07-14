package com.quip.backend.member.service;

import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.mapper.database.MemberMapper;
import com.quip.backend.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    private AuthorizationService authorizationService;

    private final MemberMapper memberMapper;


    public void validateMember(Long memberId, String operation) {
        if (!this.isMemberExists(memberId)) {
            throw new ValidationException(operation, "memberId", "must refer to an existing member");
        }
        log.info("Validated memberId: {}", memberId);
    }

    private boolean isMemberExists(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        return member != null;
    }
}
