package com.learnmate.backend.reward.repository;
import com.learnmate.backend.reward.entity.RewardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    Page<RewardTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable p);
    Optional<RewardTransaction> findByIdempotencyKey(String key);
    boolean existsByIdempotencyKey(String key);
    @Query("SELECT COALESCE(SUM(t.amount),0) FROM RewardTransaction t WHERE t.user.id = :userId")
    int sumBalanceByUserId(Long userId);
}
