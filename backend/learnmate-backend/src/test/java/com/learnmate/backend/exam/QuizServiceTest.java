package com.learnmate.backend.exam;

import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.repository.*;
import com.learnmate.backend.exam.service.QuizService;
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
class QuizServiceTest {

    @Autowired QuizService quizService;
    @Autowired AuthService authService;
    @Autowired QuizRepository quizRepo;

    private Long userId;
    private Long quizId;

    @BeforeEach
    void setup() {
        userId = authService.register(new RegisterRequest("Quiz User", "quiz@test.com", "pass12345")).userId();

        Quiz quiz = new Quiz();
        quiz.setTitle("Test Quiz");
        quiz.setDurationMinutes(30);
        quiz.setActive(true);

        QuizQuestion q = new QuizQuestion();
        q.setQuiz(quiz);
        q.setStem("What is 2+2?");
        q.setMarks(1);

        QuizOption correct = new QuizOption();
        correct.setQuestion(q); correct.setText("4"); correct.setCorrect(true); correct.setOptionIndex(0);

        QuizOption wrong = new QuizOption();
        wrong.setQuestion(q); wrong.setText("5"); wrong.setCorrect(false); wrong.setOptionIndex(1);

        q.getOptions().add(correct);
        q.getOptions().add(wrong);
        quiz.getQuestions().add(q);

        quizId = quizRepo.save(quiz).getId();
    }

    @Test
    void startAttempt_createsInProgress() {
        var attempt = quizService.startAttempt(userId, quizId, false);
        assertThat(attempt.getStatus()).isEqualTo(QuizAttempt.Status.IN_PROGRESS);
        assertThat(attempt.getDeadlineAt()).isNotNull(); // timed
    }

    @Test
    void startAttempt_untimed_noDeadline() {
        var attempt = quizService.startAttempt(userId, quizId, true);
        assertThat(attempt.getDeadlineAt()).isNull();
    }

    @Test
    void submit_scoredServerSide() {
        var attempt = quizService.startAttempt(userId, quizId, false);
        Long attemptId = attempt.getId();

        // Get question id from quiz
        Quiz quiz = quizRepo.findById(quizId).get();
        Long questionId = quiz.getQuestions().get(0).getId();
        Long correctOptionId = quiz.getQuestions().get(0).getOptions().stream()
                .filter(QuizOption::isCorrect).findFirst().get().getId();

        quizService.saveAnswer(userId, attemptId, questionId, correctOptionId);
        var submitted = quizService.submit(userId, attemptId);

        assertThat(submitted.getStatus()).isEqualTo(QuizAttempt.Status.SUBMITTED);
        assertThat(submitted.getScore()).isEqualTo(1);
        assertThat(submitted.getMaxScore()).isEqualTo(1);
    }

    @Test
    void duplicateActiveAttempt_throws409() {
        quizService.startAttempt(userId, quizId, false);
        assertThatThrownBy(() -> quizService.startAttempt(userId, quizId, false))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void submitAlreadySubmitted_throws409() {
        var attempt = quizService.startAttempt(userId, quizId, false);
        quizService.submit(userId, attempt.getId());
        assertThatThrownBy(() -> quizService.submit(userId, attempt.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
