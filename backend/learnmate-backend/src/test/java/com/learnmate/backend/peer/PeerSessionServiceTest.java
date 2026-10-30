package com.learnmate.backend.peer;

import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.peer.entity.PeerSession;
import com.learnmate.backend.peer.service.PeerSessionService;
import com.learnmate.backend.reward.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PeerSessionServiceTest {

    @Autowired PeerSessionService peerService;
    @Autowired AuthService authService;
    @Autowired RewardService rewardService;

    private Long learnerId;
    private Long teacherId;

    @BeforeEach
    void setup() {
        learnerId = authService.register(new RegisterRequest("Learner", "learner@test.com", "pass12345")).userId();
        teacherId = authService.register(new RegisterRequest("Teacher", "teacher@test.com", "pass12345")).userId();
    }

    @Test
    void fullLifecycle_completesAndDoesNotDoubleReward() {
        // Request
        PeerSession session = peerService.requestSession(learnerId, "Java basics", "Help needed", 30);
        assertThat(session.getStatus()).isEqualTo(PeerSession.Status.REQUESTED);

        // Accept
        session = peerService.acceptSession(teacherId, session.getId());
        assertThat(session.getStatus()).isEqualTo(PeerSession.Status.ACCEPTED);

        // Start
        session = peerService.startSession(teacherId, session.getId());
        assertThat(session.getStatus()).isEqualTo(PeerSession.Status.IN_PROGRESS);

        // Both confirm
        peerService.confirmCompletion(learnerId, session.getId(), 5, "Great session");
        session = peerService.confirmCompletion(teacherId, session.getId(), null, null);
        assertThat(session.getStatus()).isEqualTo(PeerSession.Status.COMPLETED);

        // In this test the session started/ended nearly immediately so actualMinutes=0
        // → below minMinutes=5 → no reward issued (correct behavior)
        assertThat(session.isRewardIssued()).isFalse();
    }

    @Test
    void teacherCannotAcceptOwnRequest() {
        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        assertThatThrownBy(() -> peerService.acceptSession(learnerId, session.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void acceptAlreadyAccepted_throws409() {
        Long teacher2 = authService.register(new RegisterRequest("T2", "t2@test.com", "pass12345")).userId();
        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        peerService.acceptSession(teacherId, session.getId());
        assertThatThrownBy(() -> peerService.acceptSession(teacher2, session.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void dispute_changesStatus() {
        PeerSession session = peerService.requestSession(learnerId, "Topic", null, 30);
        peerService.acceptSession(teacherId, session.getId());
        peerService.startSession(teacherId, session.getId());
        session = peerService.dispute(learnerId, session.getId(), "No show");
        assertThat(session.getStatus()).isEqualTo(PeerSession.Status.DISPUTED);
    }
}
