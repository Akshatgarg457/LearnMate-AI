package com.learnmate.backend.user.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.user.dto.ChangePasswordRequest;
import com.learnmate.backend.user.dto.UpdateProfileRequest;
import com.learnmate.backend.user.dto.UserResponse;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findActive(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findActive(userId);
        if (req.displayName() != null) user.setDisplayName(req.displayName().trim());
        if (req.bio() != null) user.setBio(req.bio().trim());
        if (req.timezone() != null) user.setTimezone(req.timezone().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = findActive(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    private User findActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getStatus() != User.AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Account is not active");
        }
        return user;
    }

    /** Used by admin endpoints. */
    public UserResponse getById(Long userId) {
        return UserResponse.from(userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found")));
    }
}
