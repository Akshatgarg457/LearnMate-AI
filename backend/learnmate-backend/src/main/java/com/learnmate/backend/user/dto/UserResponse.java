package com.learnmate.backend.user.dto;

import com.learnmate.backend.user.entity.User;

import java.time.Instant;

/**
 * Safe public view of a user. Never includes passwordHash.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName,
        String role,
        String status,
        String bio,
        String avatarUrl,
        String timezone,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getTimezone(),
                user.getCreatedAt()
        );
    }
}
