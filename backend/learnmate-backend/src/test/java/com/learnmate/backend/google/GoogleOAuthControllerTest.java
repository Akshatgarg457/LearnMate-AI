package com.learnmate.backend.google;

import com.learnmate.backend.auth.controller.GoogleOAuthController;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.google.dto.AuthorizationUrlResponse;
import com.learnmate.backend.google.dto.GoogleConnectionStatus;
import com.learnmate.backend.google.service.GoogleOAuthService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleOAuthControllerTest {

    private GoogleOAuthService oauthService;
    private JwtUtil jwtUtil;
    private GoogleOAuthController controller;

    @BeforeEach
    void setUp() {
        oauthService = mock(GoogleOAuthService.class);
        jwtUtil = mock(JwtUtil.class);
        controller = new GoogleOAuthController(oauthService, jwtUtil);
    }

    @Test
    void initiateRequiresBearerToken() {
        assertThatThrownBy(() -> controller.initiate(null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> controller.initiate("Basic abc"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(oauthService);
    }

    @Test
    void initiateRejectsInvalidJwt() {
        when(jwtUtil.extractUserId("bad")).thenThrow(new JwtException("bad signature"));

        assertThatThrownBy(() -> controller.initiate("Bearer bad"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(oauthService);
    }

    @Test
    void initiateReturnsConsentUrlForTheAuthenticatedUser() {
        when(jwtUtil.extractUserId("good")).thenReturn(42L);
        when(oauthService.initiate(42L))
                .thenReturn(new AuthorizationUrlResponse("https://accounts.google.com/o/oauth2/v2/auth?state=x", 600));

        ApiResponse<AuthorizationUrlResponse> response = controller.initiate("Bearer good");

        assertThat(response.getData().authorizationUrl()).startsWith("https://accounts.google.com/");
        verify(oauthService).initiate(42L);
    }

    @Test
    void statusNeverReturnsTokens() {
        when(jwtUtil.extractUserId("good")).thenReturn(42L);
        when(oauthService.isConfigured()).thenReturn(true);
        when(oauthService.isConnected(42L)).thenReturn(true);

        ApiResponse<GoogleConnectionStatus> response = controller.status("Bearer good");

        assertThat(response.getData()).isEqualTo(new GoogleConnectionStatus(true, true));
    }

    @Test
    void callbackRedirectsToTheConfiguredFrontend() {
        when(oauthService.handleCallback("code", "state", null))
                .thenReturn("http://localhost:5173/peer-learning?google=connected");

        ResponseEntity<Void> response = controller.callback("code", "state", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:5173/peer-learning?google=connected");
    }
}
