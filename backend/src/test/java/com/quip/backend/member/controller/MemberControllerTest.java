package com.quip.backend.member.controller;

import com.quip.backend.common.BaseTest;
import com.quip.backend.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link MemberController}.
 * <p>
 * This test class validates the member controller functionality.
 * Currently, the controller has no endpoints but this test ensures proper instantiation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController Tests")
public class MemberControllerTest extends BaseTest {

    @InjectMocks
    private MemberController memberController;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("Should instantiate controller successfully")
    void shouldInstantiateController_Successfully() {
        // When & Then
        assertNotNull(memberController);
        assertNotNull(memberService);
    }
}
