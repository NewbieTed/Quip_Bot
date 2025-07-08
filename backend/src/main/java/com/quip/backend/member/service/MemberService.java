package com.quip.backend.member.service;

import com.quip.backend.member.mapper.database.MemberMapper;
import com.quip.backend.member.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper memberMapper;

    public boolean isMemberExists(Long id) {
        Member member = memberMapper.findById(id);
        return member != null;
    }
}
