package com.quip.backend.member.controller;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing members.
 * <p>
 * This controller provides endpoints for member-related operations.
 * It handles HTTP requests related to member management and delegates
 * business logic to the MemberService.
 * </p>
 */
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
    /**
     * Service for member-related operations.
     */
    private final MemberService memberService;


}
