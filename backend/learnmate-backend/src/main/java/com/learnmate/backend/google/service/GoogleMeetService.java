package com.learnmate.backend.google.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.google.dto.MeetingSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Creates Google Meet spaces with the accepting teacher's own OAuth token.
 * A meeting URI is only persisted when Google actually returns one.
 */
@Service
public class GoogleMeetService {

    private static final Logger log = LoggerFactory.getLogger(GoogleMeetService.class);
    private static final String MEET_URI_PREFIX = "https://meet.google.com/";

    private final GoogleOAuthService oauthService;
    private final RestTemplate restTemplate;
    private final String spacesUrl;

    public GoogleMeetService(GoogleOAuthService oauthService,
                             @Qualifier("googleRestTemplate") RestTemplate restTemplate,
                             @Value("${app.google.oauth.meet-spaces-url}") String spacesUrl) {
        this.oauthService = oauthService;
        this.restTemplate = restTemplate;
        this.spacesUrl = spacesUrl;
    }

    public boolean isConfigured() {
        return oauthService.isConfigured();
    }

    public boolean isConnected(Long userId) {
        return oauthService.isConnected(userId);
    }

    @SuppressWarnings("unchecked")
    public MeetingSpace createSpace(Long userId) {
        String accessToken = oauthService.getAccessToken(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    spacesUrl, HttpMethod.POST, new HttpEntity<>(Map.of(), headers), Map.class);
            Map<String, Object> body = response.getBody();
            String spaceName = body == null ? null : string(body.get("name"));
            String meetingUri = body == null ? null : string(body.get("meetingUri"));

            if (spaceName == null || spaceName.isBlank() || meetingUri == null || meetingUri.isBlank()) {
                log.warn("Google Meet returned a space without a meetingUri");
                throw ApiException.serviceUnavailable("Google did not return a meeting link. Please try again");
            }
            if (!meetingUri.startsWith(MEET_URI_PREFIX)) {
                log.warn("Google Meet returned an unexpected meetingUri host");
                throw ApiException.serviceUnavailable("Google returned an unexpected meeting link");
            }
            return new MeetingSpace(spaceName, meetingUri);
        } catch (ApiException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            log.warn("Google Meet space creation failed with HTTP {}", status);
            if (status == 401) {
                throw ApiException.conflict("Google authorization expired. Please reconnect your Google account");
            }
            if (status == 403) {
                throw ApiException.conflict(
                        "Google did not allow meeting creation for this account. Reconnect Google and grant the Meet permission");
            }
            if (status == 429) {
                throw ApiException.serviceUnavailable("Google Meet is rate limiting requests. Try again shortly");
            }
            throw ApiException.serviceUnavailable("Google Meet could not create the meeting (" + status + ")");
        } catch (RestClientException e) {
            log.warn("Google Meet request failed: {}", e.getClass().getSimpleName());
            throw ApiException.serviceUnavailable("Could not reach Google Meet. Try again shortly");
        }
    }

    private static String string(Object value) {
        return value instanceof String s ? s : null;
    }
}
