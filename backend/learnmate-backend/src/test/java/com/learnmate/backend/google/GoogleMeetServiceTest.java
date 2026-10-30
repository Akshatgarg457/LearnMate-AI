package com.learnmate.backend.google;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.google.dto.MeetingSpace;
import com.learnmate.backend.google.service.GoogleMeetService;
import com.learnmate.backend.google.service.GoogleOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleMeetServiceTest {

    private static final String SPACES_URL = "https://meet.googleapis.com/v2/spaces";

    private GoogleOAuthService oauthService;
    private MockRestServiceServer server;
    private GoogleMeetService meetService;

    @BeforeEach
    void setUp() {
        oauthService = mock(GoogleOAuthService.class);
        when(oauthService.getAccessToken(5L)).thenReturn("ya29.user-token");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        meetService = new GoogleMeetService(oauthService, restTemplate, SPACES_URL);
    }

    @Test
    void createsSpaceWithTheUsersBearerToken() {
        server.expect(requestTo(SPACES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer ya29.user-token"))
                .andExpect(content().json("{}"))
                .andRespond(withSuccess("""
                        {"name":"spaces/abc-123","meetingUri":"https://meet.google.com/abc-defg-hij",
                         "meetingCode":"abc-defg-hij"}
                        """, MediaType.APPLICATION_JSON));

        MeetingSpace space = meetService.createSpace(5L);

        assertThat(space.spaceName()).isEqualTo("spaces/abc-123");
        assertThat(space.meetingUri()).isEqualTo("https://meet.google.com/abc-defg-hij");
        server.verify();
    }

    @Test
    void rejectsResponseWithoutMeetingUriInsteadOfInventingOne() {
        server.expect(requestTo(SPACES_URL))
                .andRespond(withSuccess("{\"name\":\"spaces/abc-123\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).contains("did not return a meeting link");
                });
    }

    @Test
    void rejectsUnexpectedMeetingUriHost() {
        server.expect(requestTo(SPACES_URL))
                .andRespond(withSuccess("""
                        {"name":"spaces/abc","meetingUri":"https://evil.example.com/abc"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void reportsReconnectOnUnauthorized() {
        server.expect(requestTo(SPACES_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("reconnect");
                });
    }

    @Test
    void reportsReconnectOnForbidden() {
        server.expect(requestTo(SPACES_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reportsRateLimitAsUnavailable() {
        server.expect(requestTo(SPACES_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).contains("rate limiting");
                });
    }

    @Test
    void reportsGoogleServerErrorsAsUnavailable() {
        server.expect(requestTo(SPACES_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void reportsNetworkFailureAsUnavailable() {
        server.expect(requestTo(SPACES_URL))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> meetService.createSpace(5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
