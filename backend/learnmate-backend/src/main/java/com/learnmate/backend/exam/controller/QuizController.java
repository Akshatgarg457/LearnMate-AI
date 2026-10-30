package com.learnmate.backend.exam.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.exam.dto.*;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam")
public class QuizController {

    private final QuizService service;
    private final JwtUtil jwtUtil;

    public QuizController(QuizService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/quizzes")
    public ApiResponse<Page<Quiz>> listQuizzes(
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listQuizzes(subject, PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/quizzes/{id}")
    public ApiResponse<Quiz> getQuiz(@PathVariable Long id) {
        return ApiResponse.ok(service.getQuiz(id));
    }

    @PostMapping("/quizzes/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizAttempt> startAttempt(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @RequestBody(required = false) StartQuizRequest req) {
        boolean untimed = req != null && req.untimed();
        return ApiResponse.ok(service.startAttempt(uid(auth), id, untimed));
    }

    @PutMapping("/attempts/{attemptId}/answers")
    public ApiResponse<QuizAnswer> saveAnswer(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId,
            @Valid @RequestBody SaveAnswerRequest req) {
        return ApiResponse.ok(service.saveAnswer(uid(auth), attemptId, req.questionId(), req.optionId()));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<QuizAttempt> submit(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId) {
        return ApiResponse.ok(service.submit(uid(auth), attemptId));
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<QuizAttempt> getAttempt(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId) {
        return ApiResponse.ok(service.getAttempt(uid(auth), attemptId));
    }

    @GetMapping("/attempts")
    public ApiResponse<Page<QuizAttempt>> getHistory(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistory(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
