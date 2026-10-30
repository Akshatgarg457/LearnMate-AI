package com.learnmate.backend.peer.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.google.dto.MeetingSpace;
import com.learnmate.backend.google.service.GoogleMeetService;
import com.learnmate.backend.peer.entity.PeerSession;
import com.learnmate.backend.peer.repository.PeerSessionRepository;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.service.ProgressService;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.service.RewardService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class PeerSessionService {

    private static final int MEETING_ERROR_MAX_LENGTH = 255;

    private final PeerSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final RewardService rewardService;
    private final ProgressService progressService;
    private final GoogleMeetService meetService;

    private final int pointsPerMinute;
    private final int minMinutes;
    private final int maxRewardableMinutes;

    public PeerSessionService(PeerSessionRepository sessionRepo,
                               UserRepository userRepo,
                               RewardService rewardService,
                               ProgressService progressService,
                               GoogleMeetService meetService,
                               @Value("${app.reward.points-per-teaching-minute}") int pointsPerMinute,
                               @Value("${app.reward.min-session-minutes}") int minMinutes,
                               @Value("${app.reward.max-rewardable-minutes}") int maxRewardableMinutes) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.rewardService = rewardService;
        this.progressService = progressService;
        this.meetService = meetService;
        this.pointsPerMinute = pointsPerMinute;
        this.minMinutes = minMinutes;
        this.maxRewardableMinutes = maxRewardableMinutes;
    }

    @Transactional
    public PeerSession requestSession(Long learnerId, String topic, String description, int requestedMinutes) {
        User learner = userRepo.findById(learnerId).orElseThrow(() -> ApiException.notFound("User not found"));
        PeerSession session = new PeerSession();
        session.setLearner(learner);
        session.setTopic(topic);
        session.setDescription(description);
        session.setRequestedMinutes(requestedMinutes);
        session.setStatus(PeerSession.Status.REQUESTED);
        return sessionRepo.save(session);
    }

    @Transactional
    public PeerSession acceptSession(Long teacherId, Long sessionId) {
        PeerSession session = sessionRepo.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (session.getStatus() != PeerSession.Status.REQUESTED)
            throw ApiException.conflict("Session is no longer available to accept");
        if (session.getLearner().getId().equals(teacherId))
            throw ApiException.badRequest("You cannot teach your own session");

        // Missing authorization is a precondition: leave the request open so the
        // teacher can connect Google and try again.
        boolean meetConfigured = meetService.isConfigured();
        if (meetConfigured && !meetService.isConnected(teacherId)) {
            throw ApiException.conflict(
                    "Connect your Google account before accepting so a Google Meet link can be created");
        }

        User teacher = userRepo.findById(teacherId).orElseThrow(() -> ApiException.notFound("User not found"));
        session.setTeacher(teacher);
        session.setStatus(PeerSession.Status.ACCEPTED);

        // Acceptance is kept even if Google fails; the link can be retried later.
        if (meetConfigured) {
            try {
                applyMeeting(session, meetService.createSpace(teacherId));
            } catch (ApiException e) {
                markMeetingFailed(session, e.getMessage());
            }
        }
        return sessionRepo.save(session);
    }

    /**
     * Creates the Meet space for an accepted session. Idempotent: an existing
     * link is returned instead of calling Google again.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public PeerSession createMeeting(Long teacherId, Long sessionId) {
        PeerSession session = sessionRepo.findByIdAndTeacherIdForUpdate(sessionId, teacherId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));

        if (session.getStatus() != PeerSession.Status.ACCEPTED
                && session.getStatus() != PeerSession.Status.IN_PROGRESS) {
            throw ApiException.conflict("A meeting can only be created for an accepted session");
        }
        if (session.getMeetingUri() != null && !session.getMeetingUri().isBlank()) {
            return session;
        }
        if (!meetService.isConfigured()) {
            throw ApiException.serviceUnavailable("Google Meet is not configured on this server");
        }
        if (!meetService.isConnected(teacherId)) {
            throw ApiException.conflict("Connect your Google account before creating a meeting link");
        }

        try {
            applyMeeting(session, meetService.createSpace(teacherId));
        } catch (ApiException e) {
            markMeetingFailed(session, e.getMessage());
            sessionRepo.saveAndFlush(session);
            throw e;
        }
        return sessionRepo.save(session);
    }

    @Transactional
    public PeerSession startSession(Long teacherId, Long sessionId) {
        PeerSession session = getTeacherSession(teacherId, sessionId);
        if (session.getStatus() != PeerSession.Status.ACCEPTED)
            throw ApiException.conflict("Session must be accepted before starting");
        session.setStatus(PeerSession.Status.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        return sessionRepo.save(session);
    }

    @Transactional
    public PeerSession confirmCompletion(Long userId, Long sessionId, Integer rating, String feedback) {
        PeerSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (session.getStatus() != PeerSession.Status.IN_PROGRESS)
            throw ApiException.conflict("Session is not in progress");

        boolean isLearner = session.getLearner().getId().equals(userId);
        boolean isTeacher = session.getTeacher() != null && session.getTeacher().getId().equals(userId);
        if (!isLearner && !isTeacher) throw ApiException.forbidden("Not a participant");

        if (isLearner) {
            session.setLearnerConfirmed(true);
            if (rating != null) session.setLearnerRating(rating);
            if (feedback != null) session.setLearnerFeedback(feedback);
        } else {
            session.setTeacherConfirmed(true);
        }

        if (session.isLearnerConfirmed() && session.isTeacherConfirmed()) {
            session.setStatus(PeerSession.Status.COMPLETED);
            session.setEndedAt(Instant.now());
            computeActualMinutes(session);
            session = sessionRepo.save(session);
            issueTeachingReward(session);
            progressService.recordActivity(session.getLearner().getId(),
                    LearningActivity.ActivityType.PEER_SESSION_LEARNER, session.getId(), 10);
            progressService.recordActivity(session.getTeacher().getId(),
                    LearningActivity.ActivityType.PEER_SESSION_TEACHER, session.getId(), 10);
        } else {
            session = sessionRepo.save(session);
        }
        return session;
    }

    @Transactional
    public PeerSession dispute(Long userId, Long sessionId, String reason) {
        PeerSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        boolean participant = session.getLearner().getId().equals(userId)
                || (session.getTeacher() != null && session.getTeacher().getId().equals(userId));
        if (!participant) throw ApiException.forbidden("Not a participant");
        session.setStatus(PeerSession.Status.DISPUTED);
        return sessionRepo.save(session);
    }

    public Page<PeerSession> listRequests(Pageable pageable) {
        return sessionRepo.findByStatusOrderByCreatedAtDesc(PeerSession.Status.REQUESTED, pageable);
    }

    public Page<PeerSession> getMyHistory(Long userId, Pageable pageable) {
        return sessionRepo.findByParticipant(userId, pageable);
    }

    public Page<PeerSession> getLearnerHistory(Long userId, Pageable pageable) {
        return sessionRepo.findByLearnerIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<PeerSession> getTeacherHistory(Long userId, Pageable pageable) {
        return sessionRepo.findByTeacherIdOrderByCreatedAtDesc(userId, pageable);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void applyMeeting(PeerSession session, MeetingSpace space) {
        session.setMeetingUri(space.meetingUri());
        session.setMeetingSpaceName(space.spaceName());
        session.setMeetingCreatedAt(Instant.now());
        session.setMeetingStatus(PeerSession.MeetingStatus.CREATED);
        session.setMeetingError(null);
    }

    private void markMeetingFailed(PeerSession session, String message) {
        session.setMeetingStatus(PeerSession.MeetingStatus.FAILED);
        session.setMeetingError(truncate(message));
    }

    private static String truncate(String message) {
        if (message == null) return null;
        return message.length() <= MEETING_ERROR_MAX_LENGTH
                ? message
                : message.substring(0, MEETING_ERROR_MAX_LENGTH - 3) + "...";
    }

    private void computeActualMinutes(PeerSession session) {
        if (session.getStartedAt() != null && session.getEndedAt() != null) {
            long mins = Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes();
            session.setActualMinutes((int) mins);
        }
    }

    private void issueTeachingReward(PeerSession session) {
        if (session.isRewardIssued()) return;
        if (session.getTeacher() == null) return;

        int actual = session.getActualMinutes() != null ? session.getActualMinutes() : 0;
        if (actual < minMinutes) return; // below minimum — no reward
        if (session.getLearnerRating() == null || session.getLearnerRating() < 1) return; // no feedback

        int rewardableMinutes = Math.min(actual, maxRewardableMinutes);
        int points = rewardableMinutes * pointsPerMinute;

        String idem = "teaching-reward-" + session.getId();
        rewardService.credit(session.getTeacher().getId(), points,
                RewardTransaction.TransactionType.TEACHING_REWARD,
                "Teaching reward: " + rewardableMinutes + " min × " + pointsPerMinute + " pts/min",
                idem, session.getId());

        session.setRewardIssued(true);
    }

    private PeerSession getTeacherSession(Long teacherId, Long sessionId) {
        return sessionRepo.findByIdAndTeacherId(sessionId, teacherId)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
    }
}
