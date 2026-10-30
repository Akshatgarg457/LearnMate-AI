package com.learnmate.backend.peer.controller;

import com.learnmate.backend.common.response.ApiResponse;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.peer.dto.*;
import com.learnmate.backend.peer.entity.PeerSession;
import com.learnmate.backend.peer.service.PeerSessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/peer")
public class PeerSessionController {

    private final PeerSessionService service;
    private final JwtUtil jwtUtil;

    public PeerSessionController(PeerSessionService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    /** Browse open help requests (any authenticated user) */
    @GetMapping("/requests")
    public ApiResponse<Page<PeerSession>> listRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listRequests(PageRequest.of(page, Math.min(size, 50))));
    }

    /** Create a new help request (learner) */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PeerSession> createRequest(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody CreateSessionRequest req) {
        return ApiResponse.ok(service.requestSession(uid(auth), req.topic(), req.description(), req.requestedMinutes()));
    }

    /** Accept a help request (teacher) */
    @PostMapping("/requests/{id}/accept")
    public ApiResponse<PeerSession> accept(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.acceptSession(uid(auth), id));
    }

    /** Create (or return) the Google Meet link for an accepted session (teacher) */
    @PostMapping("/sessions/{id}/meeting")
    public ApiResponse<PeerSession> createMeeting(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.createMeeting(uid(auth), id));
    }

    /** Start the session (teacher) */
    @PostMapping("/sessions/{id}/start")
    public ApiResponse<PeerSession> start(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        return ApiResponse.ok(service.startSession(uid(auth), id));
    }

    /** Confirm completion (learner or teacher) */
    @PostMapping("/sessions/{id}/complete")
    public ApiResponse<PeerSession> complete(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConfirmSessionRequest req) {
        Integer rating = req != null ? req.rating() : null;
        String feedback = req != null ? req.feedback() : null;
        return ApiResponse.ok(service.confirmCompletion(uid(auth), id, rating, feedback));
    }

    /** Dispute a session */
    @PostMapping("/sessions/{id}/dispute")
    public ApiResponse<PeerSession> dispute(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String reason) {
        return ApiResponse.ok(service.dispute(uid(auth), id, reason));
    }

    /** My full session history (both sides) */
    @GetMapping("/sessions")
    public ApiResponse<Page<PeerSession>> myHistory(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getMyHistory(uid(auth), PageRequest.of(page, Math.min(size, 50))));
    }

    private Long uid(String auth) { return jwtUtil.extractUserId(auth.substring(7)); }
}
