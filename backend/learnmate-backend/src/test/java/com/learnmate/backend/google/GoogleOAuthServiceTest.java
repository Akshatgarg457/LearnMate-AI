package com.learnmate.backend.google;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.google.crypto.TokenCipher;
import com.learnmate.backend.google.dto.AuthorizationUrlResponse;
import com.learnmate.backend.google.entity.GoogleOAuthState;
import com.learnmate.backend.google.entity.GoogleOAuthToken;
import com.learnmate.backend.google.repository.GoogleOAuthStateRepository;
import com.learnmate.backend.google.repository.GoogleOAuthTokenRepository;
import com.learnmate.backend.google.service.GoogleOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleOAuthServiceTest {

    private static final String FRONTEND = "http://localhost:5173";
    private static final String REDIRECT = "http://localhost:8080/api/v1/auth/google/callback";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/meetings.space.created";

    private GoogleOAuthStateRepository stateRepo;
    private GoogleOAuthTokenRepository tokenRepo;
    private TokenCipher cipher;
    private MockRestServiceServer server;
    private GoogleOAuthService service;

    @BeforeEach
    void setUp() {
        stateRepo = mock(GoogleOAuthStateRepository.class);
        tokenRepo = mock(GoogleOAuthTokenRepository.class);
        cipher = new TokenCipher("unit-test-encryption-key");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = buildService(restTemplate, "client-id.apps.googleusercontent.com", "client-secret");
    }

    private GoogleOAuthService buildService(RestTemplate restTemplate, String clientId, String clientSecret) {
        return new GoogleOAuthService(stateRepo, tokenRepo, cipher, restTemplate,
                clientId, clientSecret, REDIRECT, FRONTEND,
                "https://accounts.google.com/o/oauth2/v2/auth", TOKEN_URL, SCOPE, 600);
    }

    @Test
    void initiateStoresStateAndBuildsConsentUrlWithMeetScopeOnly() {
        when(stateRepo.save(any(GoogleOAuthState.class))).thenAnswer(i -> i.getArgument(0));

        AuthorizationUrlResponse response = service.initiate(7L);

        assertThat(response.authorizationUrl())
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains("client_id=client-id.apps.googleusercontent.com")
                .contains("redirect_uri=")
                .contains("response_type=code")
                .contains("access_type=offline")
                .contains("prompt=consent")
                .contains("state=")
                .contains("meetings.space.created")
                .doesNotContain("userinfo")
                .doesNotContain("gmail");
        assertThat(response.expiresInSeconds()).isEqualTo(600);

        ArgumentCaptor<GoogleOAuthState> captor = ArgumentCaptor.forClass(GoogleOAuthState.class);
        verify(stateRepo).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now().plusSeconds(500));
    }

    @Test
    void initiateFailsWhenGoogleIsNotConfigured() {
        GoogleOAuthService unconfigured = buildService(new RestTemplate(), "NOT_CONFIGURED", "NOT_CONFIGURED");

        assertThatThrownBy(() -> unconfigured.initiate(1L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(stateRepo, tokenRepo);
    }

    @Test
    void callbackRejectsUnknownState() {
        when(stateRepo.findByStateForUpdate("unknown")).thenReturn(Optional.empty());

        assertThat(service.handleCallback("code", "unknown", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=error&reason=invalid_state");
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void callbackRejectsExpiredStateAndConsumesIt() {
        GoogleOAuthState expired = oauthState("expired", 5L, Instant.now().minusSeconds(1));
        when(stateRepo.findByStateForUpdate("expired")).thenReturn(Optional.of(expired));

        assertThat(service.handleCallback("code", "expired", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=error&reason=invalid_state");
        verify(stateRepo).delete(expired);
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void callbackRedirectsWhenGoogleReturnsAnError() {
        assertThat(service.handleCallback(null, null, "access_denied"))
                .isEqualTo(FRONTEND + "/peer-learning?google=error&reason=access_denied");
        verifyNoInteractions(stateRepo, tokenRepo);
    }

    @Test
    void callbackExchangesCodeAndStoresEncryptedTokens() {
        GoogleOAuthState stored = oauthState("state-1", 7L, Instant.now().plusSeconds(300));
        when(stateRepo.findByStateForUpdate("state-1")).thenReturn(Optional.of(stored));
        when(tokenRepo.findByUserId(7L)).thenReturn(Optional.empty());

        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=authorization_code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=the-code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=client-secret")))
                .andRespond(withSuccess("""
                        {"access_token":"ya29.access","refresh_token":"1//refresh","expires_in":3599,
                         "scope":"https://www.googleapis.com/auth/meetings.space.created","token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.handleCallback("the-code", "state-1", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=connected");

        ArgumentCaptor<GoogleOAuthToken> captor = ArgumentCaptor.forClass(GoogleOAuthToken.class);
        verify(tokenRepo).save(captor.capture());
        GoogleOAuthToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getAccessTokenEnc()).isNotEqualTo("ya29.access");
        assertThat(cipher.decrypt(saved.getAccessTokenEnc())).isEqualTo("ya29.access");
        assertThat(cipher.decrypt(saved.getRefreshTokenEnc())).isEqualTo("1//refresh");
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plusSeconds(3000));
        assertThat(saved.getScope()).isEqualTo(SCOPE);
        verify(stateRepo).delete(stored);
        server.verify();
    }

    @Test
    void callbackFailsWhenScopeWasNotGranted() {
        GoogleOAuthState stored = oauthState("state-2", 7L, Instant.now().plusSeconds(300));
        when(stateRepo.findByStateForUpdate("state-2")).thenReturn(Optional.of(stored));
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token":"ya29.access","expires_in":3599,
                         "scope":"https://www.googleapis.com/auth/drive","token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.handleCallback("code", "state-2", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=error&reason=scope_not_granted");
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void callbackFailsWhenTokenExchangeFails() {
        GoogleOAuthState stored = oauthState("state-3", 7L, Instant.now().plusSeconds(300));
        when(stateRepo.findByStateForUpdate("state-3")).thenReturn(Optional.of(stored));
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThat(service.handleCallback("code", "state-3", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=error&reason=token_exchange_failed");
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void getAccessTokenRefreshesExpiredCredentialsAndKeepsRefreshToken() {
        GoogleOAuthToken token = new GoogleOAuthToken();
        token.setUserId(7L);
        token.setAccessTokenEnc(cipher.encrypt("old-access"));
        token.setRefreshTokenEnc(cipher.encrypt("refresh-1"));
        token.setExpiresAt(Instant.now().minusSeconds(30));
        when(tokenRepo.findByUserId(7L)).thenReturn(Optional.of(token));

        server.expect(requestTo(TOKEN_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
                .andRespond(withSuccess("""
                        {"access_token":"ya29.new","expires_in":3599,"token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.getAccessToken(7L)).isEqualTo("ya29.new");
        verify(tokenRepo).save(token);
        assertThat(cipher.decrypt(token.getAccessTokenEnc())).isEqualTo("ya29.new");
        assertThat(cipher.decrypt(token.getRefreshTokenEnc())).isEqualTo("refresh-1");
        server.verify();
    }

    @Test
    void getAccessTokenAsksToReconnectWhenRefreshIsImpossible() {
        GoogleOAuthToken token = new GoogleOAuthToken();
        token.setUserId(7L);
        token.setAccessTokenEnc(cipher.encrypt("old-access"));
        token.setExpiresAt(Instant.now().minusSeconds(30));
        when(tokenRepo.findByUserId(7L)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.getAccessToken(7L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void getAccessTokenWithoutConnectionAsksToConnect() {
        when(tokenRepo.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccessToken(99L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void callbackConsumesStateOnlyOnce() {
        GoogleOAuthState stored = oauthState("one-time", 7L, Instant.now().plusSeconds(300));
        when(stateRepo.findByStateForUpdate("one-time"))
                .thenReturn(Optional.of(stored))
                .thenReturn(Optional.empty());
        when(tokenRepo.findByUserId(7L)).thenReturn(Optional.empty());
        server.expect(ExpectedCount.once(), requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token":"ya29.access","expires_in":3599,
                         "scope":"https://www.googleapis.com/auth/meetings.space.created"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.handleCallback("code", "one-time", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=connected");
        assertThat(service.handleCallback("code", "one-time", null))
                .isEqualTo(FRONTEND + "/peer-learning?google=error&reason=invalid_state");
        server.verify();
    }

    private static GoogleOAuthState oauthState(String value, Long userId, Instant expiresAt) {
        GoogleOAuthState state = new GoogleOAuthState();
        state.setState(value);
        state.setUserId(userId);
        state.setExpiresAt(expiresAt);
        return state;
    }
}
