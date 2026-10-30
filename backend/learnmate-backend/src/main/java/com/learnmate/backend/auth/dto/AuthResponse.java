package com.learnmate.backend.auth.dto;

/**
 * Returned on successful login or registration.
 * Never includes the password hash.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        String displayName,
        String role
) {
    public static AuthResponse of(String token, long expiresIn,
                                   Long userId, String email,
                                   String displayName, String role) {
        return new AuthResponse(token, "Bearer", expiresIn, userId, email, displayName, role);
    }
}
