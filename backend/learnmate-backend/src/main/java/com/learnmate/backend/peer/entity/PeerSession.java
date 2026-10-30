package com.learnmate.backend.peer.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.learnmate.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Full lifecycle of a peer-learning session.
 * State machine: REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED | DISPUTED | CANCELLED
 */
@Entity
@Table(name = "peer_sessions", indexes = {
        @Index(name = "idx_ps_learner", columnList = "learner_id"),
        @Index(name = "idx_ps_teacher", columnList = "teacher_id"),
        @Index(name = "idx_ps_status", columnList = "status")
})
public class PeerSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "status", "bio", "avatarUrl", "timezone", "createdAt", "updatedAt", "hibernateLazyInitializer", "handler"})
    private User learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @JsonIgnoreProperties({"passwordHash", "email", "role", "status", "bio", "avatarUrl", "timezone", "createdAt", "updatedAt", "hibernateLazyInitializer", "handler"})
    private User teacher;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Requested duration in minutes */
    @Column(name = "requested_minutes", nullable = false)
    private int requestedMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.REQUESTED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** Actual verified duration in minutes (computed server-side) */
    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Column(name = "learner_confirmed", nullable = false)
    private boolean learnerConfirmed = false;

    @Column(name = "teacher_confirmed", nullable = false)
    private boolean teacherConfirmed = false;

    @Column(name = "learner_rating")
    private Integer learnerRating; // 1-5

    @Column(name = "learner_feedback", columnDefinition = "TEXT")
    private String learnerFeedback;

    @Column(name = "reward_issued", nullable = false)
    private boolean rewardIssued = false;

    /** Google Meet link creation state. A link is only present when Google confirmed it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_status", length = 20)
    private MeetingStatus meetingStatus = MeetingStatus.NOT_CREATED;

    @Column(name = "meeting_uri", length = 512)
    private String meetingUri;

    @Column(name = "meeting_space_name", length = 255)
    private String meetingSpaceName;

    @Column(name = "meeting_created_at")
    private Instant meetingCreatedAt;

    /** Safe, client-facing reason when Google did not create the link. */
    @Column(name = "meeting_error", length = 255)
    private String meetingError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public enum Status {
        REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, DISPUTED, CANCELLED
    }

    public enum MeetingStatus {
        NOT_CREATED, CREATED, FAILED
    }

    public Long getId() { return id; }
    public User getLearner() { return learner; }
    public void setLearner(User u) { this.learner = u; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User u) { this.teacher = u; }
    public String getTopic() { return topic; }
    public void setTopic(String t) { this.topic = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public int getRequestedMinutes() { return requestedMinutes; }
    public void setRequestedMinutes(int r) { this.requestedMinutes = r; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant s) { this.startedAt = s; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant e) { this.endedAt = e; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer a) { this.actualMinutes = a; }
    public boolean isLearnerConfirmed() { return learnerConfirmed; }
    public void setLearnerConfirmed(boolean b) { this.learnerConfirmed = b; }
    public boolean isTeacherConfirmed() { return teacherConfirmed; }
    public void setTeacherConfirmed(boolean b) { this.teacherConfirmed = b; }
    public Integer getLearnerRating() { return learnerRating; }
    public void setLearnerRating(Integer r) { this.learnerRating = r; }
    public String getLearnerFeedback() { return learnerFeedback; }
    public void setLearnerFeedback(String f) { this.learnerFeedback = f; }
    public boolean isRewardIssued() { return rewardIssued; }
    public void setRewardIssued(boolean r) { this.rewardIssued = r; }
    /** Rows created before this column existed have no value; treat them as not created. */
    public MeetingStatus getMeetingStatus() {
        return meetingStatus == null ? MeetingStatus.NOT_CREATED : meetingStatus;
    }
    public void setMeetingStatus(MeetingStatus s) { this.meetingStatus = s; }
    public String getMeetingUri() { return meetingUri; }
    public void setMeetingUri(String u) { this.meetingUri = u; }
    public String getMeetingSpaceName() { return meetingSpaceName; }
    public void setMeetingSpaceName(String n) { this.meetingSpaceName = n; }
    public Instant getMeetingCreatedAt() { return meetingCreatedAt; }
    public void setMeetingCreatedAt(Instant t) { this.meetingCreatedAt = t; }
    public String getMeetingError() { return meetingError; }
    public void setMeetingError(String e) { this.meetingError = e; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
