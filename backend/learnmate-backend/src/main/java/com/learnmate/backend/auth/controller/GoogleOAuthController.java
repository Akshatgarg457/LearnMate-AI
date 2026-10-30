package com.learnmate.backend.auth.controller;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.google.dto.AuthorizationUrlResponse;
import com.learnmate.backend.google.dto.GoogleConnectionStatus;
import com.learnmate.backend.google.service.GoogleOAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Google Meet connection endpoints. LearnMate sign-in stays on JWT:
 * the SPA calls {@code GET /api/v1/auth/google} with its Bearer token to obtain
 * the consent URL, and the callback redirects to the frontend without tokens.
 */
@RestController
@RequestMapping("/api/v1/auth/google")
public class GoogleOAuthController {

    private final GoogleOAuthService oauthService;
    private final JwtUtil jwtUtil;

    public GoogleOAuthController(GoogleOAuthService oauthService, JwtUtil jwtUtil) {
        this.oauthService = oauthService;
        this.jwtUtil = jwtUtil;
    }

    /** Returns the Google consent URL. Requires a LearnMate Bearer token. */
    @GetMapping
    public ApiResponse<AuthorizationUrlResponse> initiate(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ApiResponse.ok(oauthService.initiate(requireUserId(auth)));
    }

    /** Tells the SPA whether the server is configured and this user connected Google. */
    @GetMapping("/status")
    public ApiResponse<GoogleConnectionStatus> status(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = requireUserId(auth);
        return ApiResponse.ok(new GoogleConnectionStatus(
                oauthService.isConfigured(), oauthService.isConnected(userId)));
    }

    /** Google redirects here. Only the short-lived state and code are present. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) {
        String location = oauthService.handleCallback(code, state, error);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(location).toString())
                .build();
    }

    private Long requireUserId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw ApiException.unauthorized("Authentication required");
        }
        try {
            Long userId = jwtUtil.extractUserId(auth.substring(7));
            if (userId == null) throw ApiException.unauthorized("Authentication required");
            return userId;
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ApiException.unauthorized("Authentication required");
        }
    }
}
