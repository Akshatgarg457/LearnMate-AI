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
import java.util.Map;

@Service
public class QuizService {

    private final QuizRepository quizRepo;
    private final QuizAttemptRepository attemptRepo;
    private final QuizAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final ProgressService progressService;

    public QuizService(QuizRepository quizRepo, QuizAttemptRepository attemptRepo,
                       QuizAnswerRepository answerRepo, UserRepository userRepo,
                       ProgressService progressService) {
        this.quizRepo = quizRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.progressService = progressService;
    }

    public Page<Quiz> listQuizzes(String subject, Pageable pageable) {
        return subject != null
                ? quizRepo.findByActiveTrueAndSubject(subject, pageable)
                : quizRepo.findByActiveTrue(pageable);
    }

    public Quiz getQuiz(Long id) {
        return quizRepo.findById(id)
                .filter(Quiz::isActive)
                .orElseThrow(() -> ApiException.notFound("Quiz not found"));
    }

    @Transactional
    public QuizAttempt startAttempt(Long userId, Long quizId, boolean untimed) {
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Quiz quiz = getQuiz(quizId);

        // Prevent duplicate in-progress attempts
        var active = attemptRepo.findByUserIdAndQuizIdAndStatus(userId, quizId, QuizAttempt.Status.IN_PROGRESS);
        if (!active.isEmpty()) throw ApiException.conflict("You already have an active attempt for this quiz");

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setStatus(QuizAttempt.Status.IN_PROGRESS);
        attempt.setMaxScore(quiz.getQuestions().stream().mapToInt(QuizQuestion::getMarks).sum());

        if (!untimed && quiz.getDurationMinutes() > 0) {
            attempt.setDeadlineAt(Instant.now().plus(quiz.getDurationMinutes(), ChronoUnit.MINUTES));
        }

        return attemptRepo.save(attempt);
    }

    @Transactional
    public QuizAnswer saveAnswer(Long userId, Long attemptId, Long questionId, Long optionId) {
        QuizAttempt attempt = getOwnedAttempt(userId, attemptId);
        enforceNotExpired(attempt);

        QuizQuestion question = attempt.getQuiz().getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Question not found in this quiz"));

        QuizOption option = optionId != null
                ? question.getOptions().stream().filter(o -> o.getId().equals(optionId)).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("Option not found in question"))
                : null;

        QuizAnswer answer = answerRepo.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> { QuizAnswer a = new QuizAnswer(); a.setAttempt(attempt); a.setQuestion(question); return a; });
        answer.setSelectedOption(option);
        return answerRepo.save(answer);
    }

    @Transactional
    public QuizAttempt submit(Long userId, Long attemptId) {
        QuizAttempt attempt = getOwnedAttempt(userId, attemptId);
        if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS) {
            throw ApiException.conflict("Attempt is not in progress");
        }

        // Auto-expire if past deadline
        if (attempt.getDeadlineAt() != null && Instant.now().isAfter(attempt.getDeadlineAt())) {
            attempt.setStatus(QuizAttempt.Status.TIMED_OUT);
        } else {
            attempt.setStatus(QuizAttempt.Status.SUBMITTED);
        }

        // Server-side scoring — frontend score is never trusted
        int totalScore = 0;
        for (QuizAnswer ans : answerRepo.findByAttemptId(attemptId)) {
            boolean correct = ans.getSelectedOption() != null && ans.getSelectedOption().isCorrect();
            int marks = correct ? ans.getQuestion().getMarks() : 0;
            ans.setCorrect(correct);
            ans.setMarksAwarded(marks);
            totalScore += marks;
            answerRepo.save(ans);
        }

        attempt.setScore(totalScore);
        attempt.setSubmittedAt(Instant.now());
        attempt = attemptRepo.save(attempt);

        progressService.recordActivity(userId, LearningActivity.ActivityType.QUIZ_COMPLETED, attempt.getId(), 15);
        return attempt;
    }

    public QuizAttempt getAttempt(Long userId, Long attemptId) {
        return getOwnedAttempt(userId, attemptId);
    }

    public Page<QuizAttempt> getHistory(Long userId, Pageable pageable) {
        return attemptRepo.findByUserIdOrderByStartedAtDesc(userId, pageable);
    }

    private QuizAttempt getOwnedAttempt(Long userId, Long attemptId) {
        return attemptRepo.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("Attempt not found"));
    }

    private void enforceNotExpired(QuizAttempt attempt) {
        if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS)
            throw ApiException.conflict("Attempt is no longer active");
        if (attempt.getDeadlineAt() != null && Instant.now().isAfter(attempt.getDeadlineAt()))
            throw ApiException.unprocessable("Quiz time has expired");
    }
}
