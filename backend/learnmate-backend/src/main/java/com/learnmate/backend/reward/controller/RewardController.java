package com.learnmate.backend.reward.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.service.RewardService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rewards")
public class RewardController {

    private final RewardService service;
    private final JwtUtil jwtUtil;

    public RewardController(RewardService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/balance")
    public ApiResponse<Map<String, Integer>> getBalance(
            @RequestHeader("Authorization") String auth) {
        int balance = service.getBalance(uid(auth));
        return ApiResponse.ok(Map.of("balance", balance));
    }

    @GetMapping("/transactions")
    public ApiResponse<Page<RewardTransaction>> getHistory(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistory(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
