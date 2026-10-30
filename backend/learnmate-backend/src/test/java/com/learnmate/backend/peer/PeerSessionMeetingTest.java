package com.learnmate.backend.peer;

import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.google.dto.MeetingSpace;
import com.learnmate.backend.google.service.GoogleMeetService;
import com.learnmate.backend.peer.entity.PeerSession;
import com.learnmate.backend.peer.repository.PeerSessionRepository;
import com.learnmate.backend.peer.service.PeerSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PeerSessionMeetingTest {

    private static final String MEETING_URI = "https://meet.google.com/abc-defg-hij";

    @Autowired PeerSessionService peerService;
    @Autowired PeerSessionRepository sessionRepo;
    @Autowired AuthService authService;

    @MockitoBean GoogleMeetService meetService;

    private Long learnerId;
    private Long teacherId;

    @BeforeEach
    void setup() {
        learnerId = authService.register(new RegisterRequest("Learner", "learner-meet@test.com", "pass12345")).userId();
        teacherId = authService.register(new RegisterRequest("Teacher", "teacher-meet@test.com", "pass12345")).userId();
    }

    @Test
    void acceptanceCreatesAndPersistsTheMeeting() {
        googleReady();
        when(meetService.createSpace(teacherId))
                .thenReturn(new MeetingSpace("spaces/abc-123", MEETING_URI));

        PeerSession session = peerService.requestSession(learnerId, "Java basics", null, 30);
        PeerSession accepted = peerService.acceptSession(teacherId, session.getId());

        assertThat(accepted.getStatus()).isEqualTo(PeerSession.Status.ACCEPTED);
        assertThat(accepted.getTeacher().getId()).isEqualTo(teacherId);
        assertThat(accepted.getMeetingStatus()).isEqualTo(PeerSession.MeetingStatus.CREATED);
        assertThat(accepted.getMeetingUri()).isEqualTo(MEETING_URI);
        assertThat(accepted.getMeetingSpaceName()).isEqualTo("spaces/abc-123");
        assertThat(accepted.getMeetingCreatedAt()).isNotNull();
        assertThat(accepted.getMeetingError()).isNull();

        PeerSession reloaded = sessionRepo.findById(session.getId()).orElseThrow();
        assertThat(reloaded.getMeetingUri()).isEqualTo(MEETING_URI);
    }

    @Test
    void acceptanceWithoutGoogleConnectionLeavesTheRequestOpen() {
        when(meetService.isConfigured()).thenReturn(true);
        when(meetService.isConnected(teacherId)).thenReturn(false);

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);

        assertThatThrownBy(() -> peerService.acceptSession(teacherId, session.getId()))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("Connect your Google account");
                });

        PeerSession reloaded = sessionRepo.findById(session.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PeerSession.Status.REQUESTED);
        assertThat(reloaded.getTeacher()).isNull();
        verify(meetService, never()).createSpace(anyLong());
    }

    @Test
    void duplicateAcceptanceDoesNotCreateASecondMeeting() {
        googleReady();
        when(meetService.createSpace(teacherId))
                .thenReturn(new MeetingSpace("spaces/abc-123", MEETING_URI));
        Long secondTeacher = authService.register(
                new RegisterRequest("Teacher 2", "teacher2-meet@test.com", "pass12345")).userId();

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        peerService.acceptSession(teacherId, session.getId());

        assertThatThrownBy(() -> peerService.acceptSession(secondTeacher, session.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(meetService, times(1)).createSpace(teacherId);
        verify(meetService, never()).createSpace(secondTeacher);
    }

    @Test
    void meetFailureStillPersistsAcceptanceAndCanBeRetried() {
        googleReady();
        when(meetService.createSpace(teacherId))
                .thenThrow(ApiException.serviceUnavailable("Could not reach Google Meet. Try again shortly"));

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        PeerSession accepted = peerService.acceptSession(teacherId, session.getId());

        assertThat(accepted.getStatus()).isEqualTo(PeerSession.Status.ACCEPTED);
        assertThat(accepted.getMeetingStatus()).isEqualTo(PeerSession.MeetingStatus.FAILED);
        assertThat(accepted.getMeetingUri()).isNull();
        assertThat(accepted.getMeetingError()).contains("Could not reach Google Meet");

        doReturn(new MeetingSpace("spaces/retry", MEETING_URI))
                .when(meetService).createSpace(teacherId);
        PeerSession retried = peerService.createMeeting(teacherId, session.getId());

        assertThat(retried.getMeetingStatus()).isEqualTo(PeerSession.MeetingStatus.CREATED);
        assertThat(retried.getMeetingUri()).isEqualTo(MEETING_URI);
        assertThat(retried.getMeetingError()).isNull();
        verify(meetService, times(2)).createSpace(teacherId);
    }

    @Test
    void retryKeepsFailedStateWhenGoogleFailsAgain() {
        googleReady();
        when(meetService.createSpace(teacherId))
                .thenThrow(ApiException.serviceUnavailable("Could not reach Google Meet. Try again shortly"));

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        peerService.acceptSession(teacherId, session.getId());

        assertThatThrownBy(() -> peerService.createMeeting(teacherId, session.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        PeerSession reloaded = sessionRepo.findById(session.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PeerSession.Status.ACCEPTED);
        assertThat(reloaded.getMeetingStatus()).isEqualTo(PeerSession.MeetingStatus.FAILED);
        assertThat(reloaded.getMeetingUri()).isNull();
    }

    @Test
    void retryIsIdempotentWhenALinkAlreadyExists() {
        googleReady();
        when(meetService.createSpace(teacherId))
                .thenReturn(new MeetingSpace("spaces/abc-123", MEETING_URI));

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        peerService.acceptSession(teacherId, session.getId());
        PeerSession again = peerService.createMeeting(teacherId, session.getId());

        assertThat(again.getMeetingUri()).isEqualTo(MEETING_URI);
        verify(meetService, times(1)).createSpace(teacherId);
    }

    @Test
    void learnerCannotCreateTheMeeting() {
        googleReady();
        when(meetService.createSpace(teacherId))
                .thenReturn(new MeetingSpace("spaces/abc-123", MEETING_URI));

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        peerService.acceptSession(teacherId, session.getId());

        assertThatThrownBy(() -> peerService.createMeeting(learnerId, session.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void unconfiguredGoogleSkippedWithoutBlockingAcceptance() {
        when(meetService.isConfigured()).thenReturn(false);

        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        PeerSession accepted = peerService.acceptSession(teacherId, session.getId());

        assertThat(accepted.getStatus()).isEqualTo(PeerSession.Status.ACCEPTED);
        assertThat(accepted.getMeetingStatus()).isEqualTo(PeerSession.MeetingStatus.NOT_CREATED);
        verify(meetService, never()).isConnected(anyLong());
        verify(meetService, never()).createSpace(anyLong());
    }

    private void googleReady() {
        when(meetService.isConfigured()).thenReturn(true);
        when(meetService.isConnected(teacherId)).thenReturn(true);
    }
}
