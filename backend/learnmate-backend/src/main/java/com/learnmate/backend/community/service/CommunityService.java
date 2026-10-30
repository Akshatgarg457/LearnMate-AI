package com.learnmate.backend.community.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.community.entity.*;
import com.learnmate.backend.community.repository.*;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.service.ProgressService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CommunityService {

    private final CommunityQuestionRepository questionRepo;
    private final CommunityAnswerRepository answerRepo;
    private final ContentReportRepository reportRepo;
    private final UserRepository userRepo;
    private final ProgressService progressService;

    public CommunityService(CommunityQuestionRepository questionRepo,
                             CommunityAnswerRepository answerRepo,
                             ContentReportRepository reportRepo,
                             UserRepository userRepo,
                             ProgressService progressService) {
        this.questionRepo = questionRepo;
        this.answerRepo = answerRepo;
        this.reportRepo = reportRepo;
        this.userRepo = userRepo;
        this.progressService = progressService;
    }

    // ── Questions ────────────────────────────────────────────────────────────

    public Page<CommunityQuestion> listQuestions(String subject, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) return questionRepo.search(keyword.trim(), pageable);
        if (subject != null) return questionRepo.findByStatusAndSubjectOrderByCreatedAtDesc(
                CommunityQuestion.Status.OPEN, subject, pageable);
        return questionRepo.findByStatusOrderByCreatedAtDesc(CommunityQuestion.Status.OPEN, pageable);
    }

    public CommunityQuestion getQuestion(Long id) {
        CommunityQuestion q = questionRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        if (q.getStatus() == CommunityQuestion.Status.REMOVED)
            throw ApiException.notFound("Question not found");
        // Increment view count
        q.setViewCount(q.getViewCount() + 1);
        return questionRepo.save(q);
    }

    @Transactional
    public CommunityQuestion postQuestion(Long userId, String title, String body, String subject, String topic) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        CommunityQuestion q = new CommunityQuestion();
        q.setAuthor(user);
        q.setTitle(title);
        q.setBody(body);
        q.setSubject(subject);
        q.setTopic(topic);
        return questionRepo.save(q);
    }

    @Transactional
    public void deleteQuestion(Long userId, Long questionId) {
        CommunityQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        if (!q.getAuthor().getId().equals(userId)) throw ApiException.forbidden("Not your question");
        q.setStatus(CommunityQuestion.Status.REMOVED);
        questionRepo.save(q);
    }

    // ── Answers ──────────────────────────────────────────────────────────────

    public Page<CommunityAnswer> listAnswers(Long questionId, Pageable pageable) {
        getQuestion(questionId); // ensure visible
        return answerRepo.findByQuestionIdAndStatusOrderByVoteCountDesc(
                questionId, CommunityAnswer.Status.VISIBLE, pageable);
    }

    @Transactional
    public CommunityAnswer postAnswer(Long userId, Long questionId, String body) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        CommunityQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        if (question.getStatus() != CommunityQuestion.Status.OPEN)
            throw ApiException.conflict("Question is closed");

        CommunityAnswer answer = new CommunityAnswer();
        answer.setQuestion(question);
        answer.setAuthor(user);
        answer.setBody(body);
        answer = answerRepo.save(answer);

        progressService.recordActivity(userId, LearningActivity.ActivityType.COMMUNITY_ANSWER, answer.getId(), 8);
        return answer;
    }

    @Transactional
    public void deleteAnswer(Long userId, Long answerId) {
        CommunityAnswer answer = answerRepo.findByIdAndAuthorId(answerId, userId)
                .orElseThrow(() -> ApiException.notFound("Answer not found"));
        answer.setStatus(CommunityAnswer.Status.REMOVED);
        answerRepo.save(answer);
    }

    @Transactional
    public CommunityAnswer voteAnswer(Long answerId, int delta) {
        CommunityAnswer answer = answerRepo.findById(answerId)
                .orElseThrow(() -> ApiException.notFound("Answer not found"));
        answer.setVoteCount(answer.getVoteCount() + delta);
        return answerRepo.save(answer);
    }

    @Transactional
    public CommunityAnswer acceptAnswer(Long userId, Long questionId, Long answerId) {
        CommunityQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        if (!question.getAuthor().getId().equals(userId))
            throw ApiException.forbidden("Only the question author can accept an answer");
        CommunityAnswer answer = answerRepo.findById(answerId)
                .orElseThrow(() -> ApiException.notFound("Answer not found"));
        if (!answer.getQuestion().getId().equals(questionId))
            throw ApiException.badRequest("Answer does not belong to this question");
        answer.setAccepted(true);
        return answerRepo.save(answer);
    }

    // ── Reports ──────────────────────────────────────────────────────────────

    @Transactional
    public ContentReport reportContent(Long reporterId, ContentReport.ContentType type,
                                        Long contentId, String reason, String details) {
        User reporter = userRepo.findById(reporterId).orElseThrow(() -> ApiException.notFound("User not found"));

        if (reportRepo.existsByReporterIdAndContentTypeAndContentId(reporterId, type, contentId))
            throw ApiException.conflict("You have already reported this content");

        ContentReport report = new ContentReport();
        report.setReporter(reporter);
        report.setContentType(type);
        report.setContentId(contentId);
        report.setReason(reason);
        report.setDetails(details);
        return reportRepo.save(report);
    }

    public Page<ContentReport> listReports(ContentReport.Status status, Pageable pageable) {
        return reportRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Transactional
    public ContentReport reviewReport(Long adminId, Long reportId, ContentReport.Status status, String notes) {
        ContentReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        report.setStatus(status);
        report.setReviewedById(adminId);
        report.setReviewerNotes(notes);
        report.setReviewedAt(Instant.now());
        return reportRepo.save(report);
    }
}
