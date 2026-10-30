package com.learnmate.backend.ai.controller;

import com.learnmate.backend.ai.dto.AiConversationSummary;
import com.learnmate.backend.ai.entity.*;
import com.learnmate.backend.ai.service.AiTutorService;
import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiTutorController {

    record CreateConversationRequest(@Size(max = 200) String title, @Size(max = 100) String subject) {}
    record SendMessageRequest(@NotBlank(message = "Content is required") @Size(max = 4000) String content) {}

    private final AiTutorService service;
    private final JwtUtil jwtUtil;

    public AiTutorController(AiTutorService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiConversation> createConversation(
            @RequestHeader("Authorization") String auth,
            @RequestBody(required = false) CreateConversationRequest req) {
        return ApiResponse.ok(service.createConversation(uid(auth),
                req != null ? req.title() : null,
                req != null ? req.subject() : null));
    }

    @GetMapping("/conversations")
    public ApiResponse<Page<AiConversationSummary>> listConversations(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listConversations(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiMessage> sendMessage(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest req) {
        return ApiResponse.ok(service.sendMessage(uid(auth), id, req.content()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<AiMessage>> getMessages(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.getMessages(uid(auth), id));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
