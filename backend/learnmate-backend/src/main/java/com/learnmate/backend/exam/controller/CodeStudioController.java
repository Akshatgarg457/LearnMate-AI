package com.learnmate.backend.exam.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.exam.dto.*;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.service.CodeStudioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exam")
public class CodeStudioController {

    private final CodeStudioService service;
    private final JwtUtil jwtUtil;

    public CodeStudioController(CodeStudioService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/problems")
    public ApiResponse<Page<Problem>> listProblems(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listProblems(uid(auth), difficulty, topic, q, status, language,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by("id"))));
    }

    @GetMapping("/problems/{id}")
    public ApiResponse<Problem> getProblem(@PathVariable Long id) {
        return ApiResponse.ok(service.getProblem(id));
    }

    @GetMapping("/problems/{id}/test-cases")
    public ApiResponse<List<TestCase>> getSampleTestCases(@PathVariable Long id) {
        return ApiResponse.ok(service.getSampleTestCases(id));
    }

    @GetMapping("/problems/{id}/starter-code")
    public ApiResponse<Map<String, String>> getStarterCodes(@PathVariable Long id) {
        return ApiResponse.ok(service.getStarterCodes(id));
    }

    @PostMapping("/problems/{id}/run")
    public ApiResponse<RunResultDto> run(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody SubmitCodeRequest req) {
        return ApiResponse.ok(service.run(uid(auth), id, req.language(), req.code()));
    }

    @PostMapping("/problems/{id}/submissions")
    public ApiResponse<SubmissionDto> submit(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody SubmitCodeRequest req) {
        return ApiResponse.ok(service.submit(uid(auth), id, req.language(), req.code()));
    }

    @GetMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionDto> getSubmission(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long submissionId) {
        return ApiResponse.ok(service.getSubmission(uid(auth), submissionId));
    }

    @GetMapping("/submissions")
    public ApiResponse<Page<SubmissionSummaryDto>> getHistory(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistory(uid(auth), problemId, status, language,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50),
                        Sort.by(Sort.Direction.DESC, "submittedAt"))));
    }

    @GetMapping("/progress")
    public ApiResponse<CodeProgressDto> getProgress(@RequestHeader("Authorization") String auth) {
        return ApiResponse.ok(service.getProgress(uid(auth)));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
