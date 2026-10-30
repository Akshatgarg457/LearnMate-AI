package com.learnmate.backend.exam.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.repository.*;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.service.ProgressService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MockExamService {

    private final MockExamRepository examRepo;
    private final MockExamAttemptRepository attemptRepo;
    private final MockExamAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final ProgressService progressService;

    public MockExamService(MockExamRepository examRepo, MockExamAttemptRepository attemptRepo,
                           MockExamAnswerRepository answerRepo, UserRepository userRepo,
                           ProgressService progressService) {
        this.examRepo = examRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.progressService = progressService;
    }

    public Page<MockExam> listExams(String subject, Pageable pageable) {
        return subject != null
                ? examRepo.findByActiveTrueAndSubject(subject, pageable)
                : examRepo.findByActiveTrue(pageable);
    }

    public MockExam getExam(Long id) {
        return examRepo.findById(id).filter(MockExam::isActive)
                .orElseThrow(() -> ApiException.notFound("Exam not found"));
    }

    @Transactional
    public MockExamAttempt startAttempt(Long userId, Long examId) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        MockExam exam = getExam(examId);

        var active = attemptRepo.findByUserIdAndExamIdAndStatus(userId, examId, MockExamAttempt.Status.IN_PROGRESS);
        if (!active.isEmpty()) throw ApiException.conflict("You already have an active attempt for this exam");

        MockExamAttempt attempt = new MockExamAttempt();
        attempt.setUser(user);
        attempt.setExam(exam);
        attempt.setStatus(MockExamAttempt.Status.IN_PROGRESS);
        attempt.setDeadlineAt(Instant.now().plus(exam.getDurationMinutes(), ChronoUnit.MINUTES));
        attempt.setMaxScore(exam.getQuestions().stream().mapToInt(MockExamQuestion::getMarks).sum());
        attempt.setPassPercentage(exam.getPassPercentage());
        return attemptRepo.save(attempt);
    }

    @Transactional
    public MockExamAnswer saveAnswer(Long userId, Long attemptId, Long questionId, Long optionId) {
        MockExamAttempt attempt = getOwnedAttempt(userId, attemptId);
        enforceActive(attempt);

        MockExamQuestion question = attempt.getExam().getQuestions().stream()
                .filter(q -> q.getId().equals(questionId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Question not found"));

        MockExamOption option = optionId != null
                ? question.getOptions().stream().filter(o -> o.getId().equals(optionId)).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("Option not found"))
                : null;

        MockExamAnswer ans = answerRepo.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> { MockExamAnswer a = new MockExamAnswer(); a.setAttempt(attempt); a.setQuestion(question); return a; });
        ans.setSelectedOption(option);
        return answerRepo.save(ans);
    }

    @Transactional
    public MockExamAttempt submit(Long userId, Long attemptId) {
        MockExamAttempt attempt = getOwnedAttempt(userId, attemptId);
        if (attempt.getStatus() != MockExamAttempt.Status.IN_PROGRESS)
            throw ApiException.conflict("Attempt is not in progress");

        boolean timedOut = Instant.now().isAfter(attempt.getDeadlineAt());
        attempt.setStatus(timedOut ? MockExamAttempt.Status.TIMED_OUT : MockExamAttempt.Status.SUBMITTED);

        int score = 0;
        for (MockExamAnswer ans : answerRepo.findByAttemptId(attemptId)) {
            boolean correct = ans.getSelectedOption() != null && ans.getSelectedOption().isCorrect();
            int marks = correct ? ans.getQuestion().getMarks() : 0;
            ans.setCorrect(correct);
            ans.setMarksAwarded(marks);
            score += marks;
            answerRepo.save(ans);
        }

        attempt.setScore(score);
        attempt.setSubmittedAt(Instant.now());
        int maxScore = attempt.getMaxScore() != null ? attempt.getMaxScore() : 1;
        double pct = maxScore > 0 ? (score * 100.0 / maxScore) : 0;
        attempt.setPassed(pct >= attempt.getPassPercentage());
        attempt = attemptRepo.save(attempt);

        progressService.recordActivity(userId, LearningActivity.ActivityType.MOCK_EXAM_COMPLETED, attempt.getId(), 30);
        return attempt;
    }

    /** Build topic-wise performance map for a submitted attempt. */
    public Map<String, int[]> getTopicPerformance(Long userId, Long attemptId) {
        MockExamAttempt attempt = getOwnedAttempt(userId, attemptId);
        Map<String, int[]> perf = new LinkedHashMap<>(); // topic -> [earned, max]
        for (MockExamAnswer ans : answerRepo.findByAttemptId(attemptId)) {
            String topic = ans.getQuestion().getTopic() != null ? ans.getQuestion().getTopic() : "General";
            perf.computeIfAbsent(topic, k -> new int[2]);
            perf.get(topic)[1] += ans.getQuestion().getMarks();
            if (Boolean.TRUE.equals(ans.getCorrect())) perf.get(topic)[0] += ans.getMarksAwarded();
        }
        return perf;
    }

    public MockExamAttempt getAttempt(Long userId, Long attemptId) {
        return getOwnedAttempt(userId, attemptId);
    }

    public Page<MockExamAttempt> getHistory(Long userId, Pageable pageable) {
        return attemptRepo.findByUserIdOrderByStartedAtDesc(userId, pageable);
    }

    private MockExamAttempt getOwnedAttempt(Long userId, Long attemptId) {
        return attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("Attempt not found"));
    }

    private void enforceActive(MockExamAttempt attempt) {
        if (attempt.getStatus() != MockExamAttempt.Status.IN_PROGRESS)
            throw ApiException.conflict("Attempt is no longer active");
        if (Instant.now().isAfter(attempt.getDeadlineAt()))
            throw ApiException.unprocessable("Exam time has expired");
    }
}
