package com.learnmate.backend.progress.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.progress.entity.*;
import com.learnmate.backend.progress.service.ProgressService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final ProgressService service;
    private final JwtUtil jwtUtil;

    public ProgressController(ProgressService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/summary")
    public ApiResponse<UserProgress> getSummary(
            @RequestHeader("Authorization") String auth) {
        return ApiResponse.ok(service.getProgress(uid(auth)));
    }

    @GetMapping("/streak")
    public ApiResponse<Map<String, Object>> getStreak(
            @RequestHeader("Authorization") String auth) {
        UserProgress p = service.getProgress(uid(auth));
        return ApiResponse.ok(Map.of(
                "currentStreak", p.getCurrentStreak(),
                "longestStreak", p.getLongestStreak(),
                "lastActivityDate", p.getLastActivityDate() != null ? p.getLastActivityDate().toString() : null
        ));
    }

    @GetMapping("/activity")
    public ApiResponse<List<Map<String, Object>>> getHeatmap(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusDays(365);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        return ApiResponse.ok(service.getHeatmap(uid(auth), fromDate, toDate));
    }

    @GetMapping("/achievements")
    public ApiResponse<List<UserAchievement>> getAchievements(
            @RequestHeader("Authorization") String auth) {
        return ApiResponse.ok(service.getAchievements(uid(auth)));
    }

    @GetMapping("/history")
    public ApiResponse<Page<LearningActivity>> getHistory(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getActivityHistory(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
