package com.learnmate.backend.community.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.community.entity.*;
import com.learnmate.backend.community.service.CommunityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {

    record PostQuestionRequest(
            @NotBlank @Size(max = 400) String title,
            @NotBlank @Size(max = 10000) String body,
            @Size(max = 100) String subject,
            @Size(max = 100) String topic) {}

    record PostAnswerRequest(
            @NotBlank @Size(max = 10000) String body) {}

    record ReportRequest(
            @NotBlank @Size(max = 100) String reason,
            @Size(max = 1000) String details) {}

    private final CommunityService service;
    private final JwtUtil jwtUtil;

    public CommunityController(CommunityService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    // ── Questions ────────────────────────────────────────────────────────────

    @GetMapping("/questions")
    public ApiResponse<Page<CommunityQuestion>> listQuestions(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listQuestions(subject, q, PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<CommunityQuestion> getQuestion(@PathVariable Long id) {
        return ApiResponse.ok(service.getQuestion(id));
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityQuestion> postQuestion(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody PostQuestionRequest req) {
        return ApiResponse.ok(service.postQuestion(uid(auth), req.title(), req.body(), req.subject(), req.topic()));
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        service.deleteQuestion(uid(auth), id);
    }

    // ── Answers ──────────────────────────────────────────────────────────────

    @GetMapping("/questions/{questionId}/answers")
    public ApiResponse<Page<CommunityAnswer>> listAnswers(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listAnswers(questionId, PageRequest.of(page, Math.min(size, 50))));
    }

    @PostMapping("/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityAnswer> postAnswer(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long questionId,
            @Valid @RequestBody PostAnswerRequest req) {
        return ApiResponse.ok(service.postAnswer(uid(auth), questionId, req.body()));
    }

    @DeleteMapping("/answers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswer(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        service.deleteAnswer(uid(auth), id);
    }

    @PostMapping("/questions/{questionId}/answers/{answerId}/accept")
    public ApiResponse<CommunityAnswer> acceptAnswer(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long questionId,
            @PathVariable Long answerId) {
        return ApiResponse.ok(service.acceptAnswer(uid(auth), questionId, answerId));
    }

    // ── Reports ──────────────────────────────────────────────────────────────

    @PostMapping("/questions/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContentReport> reportQuestion(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest req) {
        return ApiResponse.ok(service.reportContent(uid(auth), ContentReport.ContentType.QUESTION, id, req.reason(), req.details()));
    }

    @PostMapping("/answers/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContentReport> reportAnswer(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest req) {
        return ApiResponse.ok(service.reportContent(uid(auth), ContentReport.ContentType.ANSWER, id, req.reason(), req.details()));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
