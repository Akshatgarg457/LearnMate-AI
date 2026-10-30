package com.learnmate.backend.exam.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.exam.dto.SaveAnswerRequest;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.service.MockExamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/exam")
public class MockExamController {

    private final MockExamService service;
    private final JwtUtil jwtUtil;

    public MockExamController(MockExamService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/mock-exams")
    public ApiResponse<Page<MockExam>> listExams(
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listExams(subject, PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/mock-exams/{id}")
    public ApiResponse<MockExam> getExam(@PathVariable Long id) {
        return ApiResponse.ok(service.getExam(id));
    }

    @PostMapping("/mock-exams/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MockExamAttempt> startAttempt(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.startAttempt(uid(auth), id));
    }

    @PutMapping("/mock-attempts/{attemptId}/answers")
    public ApiResponse<MockExamAnswer> saveAnswer(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId,
            @Valid @RequestBody SaveAnswerRequest req) {
        return ApiResponse.ok(service.saveAnswer(uid(auth), attemptId, req.questionId(), req.optionId()));
    }

    @PostMapping("/mock-attempts/{attemptId}/submit")
    public ApiResponse<MockExamAttempt> submit(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId) {
        return ApiResponse.ok(service.submit(uid(auth), attemptId));
    }

    @GetMapping("/mock-attempts/{attemptId}")
    public ApiResponse<MockExamAttempt> getAttempt(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId) {
        return ApiResponse.ok(service.getAttempt(uid(auth), attemptId));
    }

    @GetMapping("/mock-attempts/{attemptId}/performance")
    public ApiResponse<Map<String, int[]>> getTopicPerformance(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long attemptId) {
        return ApiResponse.ok(service.getTopicPerformance(uid(auth), attemptId));
    }

    @GetMapping("/mock-attempts")
    public ApiResponse<Page<MockExamAttempt>> getHistory(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistory(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
