package com.learnmate.backend.google.repository;

import com.learnmate.backend.google.entity.GoogleOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, Long> {
    Optional<GoogleOAuthToken> findByUserId(Long userId);
}
