package com.learnmate.backend.dashboard.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.dashboard.dto.DashboardSummary;
import com.learnmate.backend.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final JwtUtil jwtUtil;

    public DashboardController(DashboardService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ApiResponse<DashboardSummary> getSummary(
            @RequestHeader("Authorization") String auth) {
        return ApiResponse.ok(service.getSummary(uid(auth)));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
