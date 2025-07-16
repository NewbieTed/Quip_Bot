package com.quip.backend.member.service;

import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.mapper.database.MemberMapper;
import com.quip.backend.member.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest extends BaseTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberMapper memberMapper;

    private static final long TEST_NUM = 10;

    @Test
    void validateMember_existingMember_noExceptions() {
        for (long i = 1L; i <= TEST_NUM; i++) {
            Member member = new Member(
                    i,
                    "Member" + i,
                    0L,
                    0L,
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
            );
            when(memberMapper.selectById(i)).thenReturn(member);
        }

        for (long i = 1L; i <= TEST_NUM; i++) {
            memberService.validateMember(i, "Test");
        }
    }

    @Test
    void validateMember_nonExistingMember_throwsException() {
        for (long i = 1L; i <= TEST_NUM; i++) {
            when(memberMapper.selectById(i)).thenReturn(null);
        }

        for (long i = 1L; i <= TEST_NUM; i++) {
            final long id = i;
            assertThrows(
                    ValidationException.class,
                    () -> memberService.validateMember(id, "Test")
            );
        }
    }
}
