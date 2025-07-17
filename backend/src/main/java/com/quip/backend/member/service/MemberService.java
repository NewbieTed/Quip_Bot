package com.quip.backend.member.service;

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

    private final MemberMapper memberMapper;

    public Member validateMember(Long memberId, String operation) {
        if (memberId == null) {
            throw new ValidationException(operation, "memberId", "must not be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Parameter 'operation' must not be null.");
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new ValidationException(operation, "memberId", "must refer to an existing member");
        }
        log.info("Validated memberId: {}", memberId);
        return member;
    }
}
