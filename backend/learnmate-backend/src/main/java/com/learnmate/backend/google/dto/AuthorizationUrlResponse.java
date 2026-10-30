package com.learnmate.backend.google.dto;

/** Authorization URL the SPA sends the browser to. Contains no LearnMate JWT. */
public record AuthorizationUrlResponse(String authorizationUrl, long expiresInSeconds) {}
