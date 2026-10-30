package com.learnmate.backend.admin.controller;

import com.learnmate.backend.admin.service.AdminService;
import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.community.entity.ContentReport;
import com.learnmate.backend.course.entity.Course;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.peer.entity.PeerSession;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.user.dto.UserResponse;
import com.learnmate.backend.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    record UpdateStatusRequest(@NotNull User.AccountStatus status) {}
    record UpdateRoleRequest(@NotNull User.Role role) {}
    record RewardAdjustRequest(int amount, @NotBlank String reason) {}
    record ToggleRequest(boolean active) {}

    private final AdminService service;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    // ── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ApiResponse<Page<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(service.listUsers(PageRequest.of(page, Math.min(size, 100))));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @PathVariable Long id, @RequestBody UpdateStatusRequest req) {
        return ApiResponse.ok(service.updateUserStatus(id, req.status()));
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<UserResponse> updateUserRole(
            @PathVariable Long id, @RequestBody UpdateRoleRequest req) {
        return ApiResponse.ok(service.updateUserRole(id, req.role()));
    }

    // ── Courses ──────────────────────────────────────────────────────────────

    @GetMapping("/courses")
    public ApiResponse<Page<Course>> listCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(service.listAllCourses(PageRequest.of(page, Math.min(size, 100))));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Course> createCourse(@RequestBody Course course) {
        return ApiResponse.ok(service.createCourse(course));
    }

    @PatchMapping("/courses/{id}")
    public ApiResponse<Course> updateCourse(@PathVariable Long id, @RequestBody Course updates) {
        return ApiResponse.ok(service.updateCourse(id, updates));
    }

    @PatchMapping("/courses/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleCourseActive(@PathVariable Long id, @RequestBody ToggleRequest req) {
        service.toggleCourseActive(id, req.active());
    }

    // ── Reports / Disputes ───────────────────────────────────────────────────

    @GetMapping("/reports")
    public ApiResponse<Page<ContentReport>> listReports(
            @RequestParam(required = false) ContentReport.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(service.listReports(status, PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/disputes")
    public ApiResponse<Page<PeerSession>> listDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(service.listDisputes(PageRequest.of(page, Math.min(size, 100))));
    }

    @PostMapping("/disputes/{id}/resolve")
    public ApiResponse<PeerSession> resolveDispute(
            @PathVariable Long id,
            @RequestParam PeerSession.Status resolution) {
        return ApiResponse.ok(service.resolveDispute(id, resolution));
    }

    // ── Rewards ──────────────────────────────────────────────────────────────

    @PostMapping("/rewards/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RewardTransaction> adjustReward(
            @RequestHeader("Authorization") String auth,
            @RequestParam Long userId,
            @RequestBody RewardAdjustRequest req) {
        return ApiResponse.ok(service.adjustReward(uid(auth), userId, req.amount(), req.reason()));
    }

    // ── Quizzes ──────────────────────────────────────────────────────────────

    @GetMapping("/quizzes")
    public ApiResponse<Page<Quiz>> listQuizzes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(service.listAllQuizzes(PageRequest.of(page, Math.min(size, 100))));
    }

    @PatchMapping("/quizzes/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleQuiz(@PathVariable Long id, @RequestBody ToggleRequest req) {
        service.toggleQuizActive(id, req.active());
    }

    // ── Mock Exams ───────────────────────────────────────────────────────────

    @GetMapping("/mock-exams")
    public ApiResponse<Page<MockExam>> listMockExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(service.listAllMockExams(PageRequest.of(page, Math.min(size, 100))));
    }

    @PatchMapping("/mock-exams/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleMockExam(@PathVariable Long id, @RequestBody ToggleRequest req) {
        service.toggleMockExamActive(id, req.active());
    }

    // ── Problems ─────────────────────────────────────────────────────────────

    @PostMapping("/problems")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Problem> createProblem(@RequestBody Problem problem) {
        return ApiResponse.ok(service.createProblem(problem));
    }

    @PatchMapping("/problems/{id}/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleProblem(@PathVariable Long id, @RequestBody ToggleRequest req) {
        service.toggleProblemActive(id, req.active());
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
