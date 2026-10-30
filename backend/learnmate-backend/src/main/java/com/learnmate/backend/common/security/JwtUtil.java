package com.learnmate.backend.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Stateless JWT helper. Tokens carry the subject (username/email),
 * a role claim, and the user-id claim.
 *
 * <p><strong>Secret management:</strong> {@code app.jwt.secret} must be
 * set via an environment variable in production. The default placeholder
 * value in application.properties is intentionally invalid for production.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiryMs;
    private final long refreshExpiryMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-ms}") long expiryMs,
            @Value("${app.jwt.refresh-expiry-ms}") long refreshExpiryMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    /** Generate an access token. */
    public String generateToken(String subject, Long userId, String role) {
        return buildToken(subject, userId, role, expiryMs);
    }

    /** Generate a refresh token (longer lived, no role claim). */
    public String generateRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiryMs))
                .signWith(key)
                .compact();
    }

    private String buildToken(String subject, Long userId, String role, long ttlMs) {
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    /** Extract the subject (email) from a token. */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extract the user ID embedded in the token. */
    public Long extractUserId(String token) {
        Object raw = parseClaims(token).get("userId");
        if (raw instanceof Integer i) return i.longValue();
        if (raw instanceof Long l) return l;
        if (raw instanceof String s) return Long.parseLong(s);
        return null;
    }

    /** Extract the role embedded in the token. */
    public String extractRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    /**
     * Returns true if the token is valid for the given UserDetails.
     * Checks subject match and expiry.
     */
    public boolean isValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Returns true if the token can be parsed and is not expired.
     * Used by the JWT filter to guard before loading UserDetails.
     */
    public boolean isTokenWellFormed(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
