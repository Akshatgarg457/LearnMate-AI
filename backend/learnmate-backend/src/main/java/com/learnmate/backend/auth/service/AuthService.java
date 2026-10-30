package com.learnmate.backend.auth.service;

import com.learnmate.backend.auth.dto.AuthResponse;
import com.learnmate.backend.auth.dto.LoginRequest;
import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.common.security.JwtUtil;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final long jwtExpiryMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authManager,
                       @Value("${app.jwt.expiry-ms}") long jwtExpiryMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
        this.jwtExpiryMs = jwtExpiryMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("An account with this email address already exists");
        }

        User user = new User();
        user.setEmail(req.email().toLowerCase().trim());
        user.setDisplayName(req.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(User.Role.STUDENT);
        user.setStatus(User.AccountStatus.ACTIVE);
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return AuthResponse.of(token, jwtExpiryMs, user.getId(),
                user.getEmail(), user.getDisplayName(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (BadCredentialsException e) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (user.getStatus() != User.AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Your account is not active. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return AuthResponse.of(token, jwtExpiryMs, user.getId(),
                user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
