package com.learnmate.backend.google.repository;

import com.learnmate.backend.google.entity.GoogleOAuthState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface GoogleOAuthStateRepository extends JpaRepository<GoogleOAuthState, String> {

    /** Serializes concurrent callbacks that present the same state. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM GoogleOAuthState s WHERE s.state = :state")
    Optional<GoogleOAuthState> findByStateForUpdate(@Param("state") String state);

    long deleteByExpiresAtBefore(Instant cutoff);
}
