package com.learnmate.backend.user.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.user.dto.ChangePasswordRequest;
import com.learnmate.backend.user.dto.UpdateProfileRequest;
import com.learnmate.backend.user.dto.UserResponse;
import com.learnmate.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * GET    /api/v1/users/me           — Current user profile
 * PATCH  /api/v1/users/me           — Update profile
 * POST   /api/v1/users/me/password  — Change password
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = extractUserId(authHeader);
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(authHeader);
        userService.changePassword(userId, request);
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}
