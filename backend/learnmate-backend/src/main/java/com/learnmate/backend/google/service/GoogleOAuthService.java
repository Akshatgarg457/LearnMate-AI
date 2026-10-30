package com.learnmate.backend.google.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.google.crypto.TokenCipher;
import com.learnmate.backend.google.dto.AuthorizationUrlResponse;
import com.learnmate.backend.google.entity.GoogleOAuthState;
import com.learnmate.backend.google.entity.GoogleOAuthToken;
import com.learnmate.backend.google.repository.GoogleOAuthStateRepository;
import com.learnmate.backend.google.repository.GoogleOAuthTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Google OAuth 2.0 Authorization Code flow. Only the Meet space scope is
 * requested; LearnMate login stays on its own JWT.
 *
 * <p>Tokens never reach the browser: the callback stores them encrypted and
 * redirects to the configured frontend with a short result code.
 */
@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String NOT_CONFIGURED = "NOT_CONFIGURED";
    private static final int STATE_BYTES = 32;

    private final GoogleOAuthStateRepository stateRepo;
    private final GoogleOAuthTokenRepository tokenRepo;
    private final TokenCipher cipher;
    private final RestTemplate restTemplate;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String frontendUrl;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String scope;
    private final long stateTtlSeconds;

    private final SecureRandom random = new SecureRandom();

    public GoogleOAuthService(
            GoogleOAuthStateRepository stateRepo,
            GoogleOAuthTokenRepository tokenRepo,
            TokenCipher cipher,
            @Qualifier("googleRestTemplate") RestTemplate restTemplate,
            @Value("${app.google.oauth.client-id}") String clientId,
            @Value("${app.google.oauth.client-secret}") String clientSecret,
            @Value("${app.google.oauth.redirect-uri}") String redirectUri,
            @Value("${app.google.oauth.frontend-url}") String frontendUrl,
            @Value("${app.google.oauth.authorize-url}") String authorizeUrl,
            @Value("${app.google.oauth.token-url}") String tokenUrl,
            @Value("${app.google.oauth.scope}") String scope,
            @Value("${app.google.oauth.state-ttl-seconds}") long stateTtlSeconds) {
        this.stateRepo = stateRepo;
        this.tokenRepo = tokenRepo;
        this.cipher = cipher;
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.frontendUrl = stripTrailingSlash(validateFrontendUrl(frontendUrl));
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.scope = scope;
        this.stateTtlSeconds = stateTtlSeconds;
    }

    public boolean isConfigured() {
        return isReal(clientId) && isReal(clientSecret);
    }

    public boolean isConnected(Long userId) {
        return tokenRepo.findByUserId(userId)
                .map(t -> t.getRefreshTokenEnc() != null
                        || (t.getExpiresAt() != null && t.getExpiresAt().isAfter(Instant.now())))
                .orElse(false);
    }

    /**
     * Creates a one-time state bound to the signed-in user and returns the
     * Google consent URL. The state is stored server-side; nothing sensitive
     * is placed in the URL except the random state value.
     */
    @Transactional
    public AuthorizationUrlResponse initiate(Long userId) {
        if (!isConfigured()) {
            throw ApiException.serviceUnavailable("Google integration is not configured on this server");
        }
        stateRepo.deleteByExpiresAtBefore(Instant.now());

        byte[] bytes = new byte[STATE_BYTES];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        GoogleOAuthState stored = new GoogleOAuthState();
        stored.setState(state);
        stored.setUserId(userId);
        stored.setExpiresAt(Instant.now().plusSeconds(stateTtlSeconds));
        stateRepo.save(stored);

        String url = UriComponentsBuilder.fromUriString(authorizeUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();

        return new AuthorizationUrlResponse(url, stateTtlSeconds);
    }

    /**
     * Validates and consumes the state, exchanges the code, stores tokens and
     * returns the frontend redirect. All failures resolve to a redirect with a
     * fixed reason code; exception details are never put in the URL.
     */
    @Transactional
    public String handleCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            return errorRedirect("access_denied".equals(error) ? "access_denied" : "google_error");
        }
        if (state == null || state.isBlank()) {
            return errorRedirect("invalid_state");
        }

        GoogleOAuthState stored;
        try {
            stored = consumeState(state);
        } catch (ApiException e) {
            return errorRedirect("invalid_state");
        }

        if (code == null || code.isBlank()) {
            return errorRedirect("missing_code");
        }

        TokenPayload tokens;
        try {
            tokens = exchangeCode(code);
        } catch (Exception e) {
            log.warn("Google token exchange failed: {}", e.getClass().getSimpleName());
            return errorRedirect("token_exchange_failed");
        }

        if (!hasRequiredScope(tokens.scope())) {
            log.warn("Google consent did not include the required Meet scope");
            return errorRedirect("scope_not_granted");
        }

        storeTokens(stored.getUserId(), tokens);
        return frontendRedirect("connected", null);
    }

    /**
     * Returns a usable access token, refreshing it when it is close to expiry.
     * Throws a conflict (which the UI turns into "reconnect Google") when the
     * user never connected or the refresh token is gone.
     */
    @Transactional
    public String getAccessToken(Long userId) {
        GoogleOAuthToken token = tokenRepo.findByUserId(userId)
                .orElseThrow(() -> ApiException.conflict(
                        "Connect your Google account before creating a meeting link"));

        if (token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return decryptOrReconnect(token.getAccessTokenEnc());
        }
        if (token.getRefreshTokenEnc() == null) {
            throw ApiException.conflict("Your Google connection expired. Please reconnect your Google account");
        }

        TokenPayload refreshed;
        try {
            refreshed = refreshAccessToken(cipher.decrypt(token.getRefreshTokenEnc()));
        } catch (RestClientException e) {
            log.warn("Google token refresh failed: {}", e.getClass().getSimpleName());
            throw ApiException.conflict("Your Google connection expired. Please reconnect your Google account");
        } catch (IllegalStateException e) {
            throw ApiException.conflict("Your Google connection expired. Please reconnect your Google account");
        }

        applyToken(token, refreshed);
        tokenRepo.save(token);
        return refreshed.accessToken();
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private GoogleOAuthState consumeState(String state) {
        GoogleOAuthState stored = stateRepo.findByStateForUpdate(state)
                .orElseThrow(() -> ApiException.badRequest("OAuth state is not valid"));
        stateRepo.delete(stored);
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("OAuth state has expired");
        }
        return stored;
    }

    private TokenPayload exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        Map<String, Object> body = postForm(form);
        return TokenPayload.from(body);
    }

    private TokenPayload refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");

        Map<String, Object> body = postForm(form);
        return TokenPayload.from(body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForm(MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        Map<String, Object> body = restTemplate.postForObject(
                tokenUrl, new HttpEntity<>(form, headers), Map.class);
        if (body == null) throw new IllegalStateException("Google returned an empty token response");
        return body;
    }

    private void storeTokens(Long userId, TokenPayload tokens) {
        GoogleOAuthToken token = tokenRepo.findByUserId(userId).orElseGet(GoogleOAuthToken::new);
        token.setUserId(userId);
        applyToken(token, tokens);
        tokenRepo.save(token);
    }

    private void applyToken(GoogleOAuthToken token, TokenPayload tokens) {
        token.setAccessTokenEnc(cipher.encrypt(tokens.accessToken()));
        if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
            token.setRefreshTokenEnc(cipher.encrypt(tokens.refreshToken()));
        }
        token.setExpiresAt(Instant.now().plusSeconds(tokens.expiresInSeconds() > 0 ? tokens.expiresInSeconds() : 3600));
        if (tokens.scope() != null && !tokens.scope().isBlank()) token.setScope(tokens.scope());
        if (tokens.tokenType() != null && !tokens.tokenType().isBlank()) token.setTokenType(tokens.tokenType());
    }

    private String decryptOrReconnect(String encrypted) {
        try {
            return cipher.decrypt(encrypted);
        } catch (IllegalStateException e) {
            throw ApiException.conflict("Your Google connection expired. Please reconnect your Google account");
        }
    }

    private boolean hasRequiredScope(String granted) {
        if (granted == null || granted.isBlank()) return true;
        return Arrays.asList(granted.trim().split("\\s+")).contains(scope);
    }

    private String frontendRedirect(String result, String reason) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/peer-learning")
                .queryParam("google", result);
        if (reason != null) builder.queryParam("reason", reason);
        return builder.build().encode().toUriString();
    }

    private String errorRedirect(String reason) {
        return frontendRedirect("error", reason);
    }

    private static boolean isReal(String value) {
        return value != null && !value.isBlank() && !NOT_CONFIGURED.equalsIgnoreCase(value.trim());
    }

    private static String validateFrontendUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Frontend URL must use http or https");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("Frontend URL must include a host");
            }
            return url;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.google.oauth.frontend-url is not a valid URL", e);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record TokenPayload(String accessToken, String refreshToken, long expiresInSeconds,
                                String scope, String tokenType) {

        static TokenPayload from(Map<String, Object> body) {
            Object access = body.get("access_token");
            if (!(access instanceof String s) || s.isBlank()) {
                throw new IllegalStateException("Google token response has no access_token");
            }
            long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 3600;
            return new TokenPayload(
                    s,
                    string(body.get("refresh_token")),
                    expiresIn,
                    string(body.get("scope")),
                    string(body.get("token_type")));
        }

        private static String string(Object value) {
            return value instanceof String s ? s : null;
        }
    }
}
