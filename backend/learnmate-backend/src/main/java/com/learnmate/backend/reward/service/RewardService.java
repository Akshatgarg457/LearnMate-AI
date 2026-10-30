package com.learnmate.backend.reward.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.repository.RewardTransactionRepository;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-authoritative reward ledger.
 * All credit/debit operations are idempotent via idempotencyKey.
 * Balances cannot go negative — enforced here, not in the DB.
 */
@Service
public class RewardService {

    private final RewardTransactionRepository txRepo;
    private final UserRepository userRepo;

    public RewardService(RewardTransactionRepository txRepo, UserRepository userRepo) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
    }

    /** Current balance for user. */
    @Transactional(readOnly = true)
    public int getBalance(Long userId) {
        return txRepo.sumBalanceByUserId(userId);
    }

    /**
     * Credit points with idempotency — safe to call multiple times with same key.
     * @return the transaction, or null if already applied (idempotent skip)
     */
    @Transactional
    public RewardTransaction credit(Long userId, int amount, RewardTransaction.TransactionType type,
                                    String description, String idempotencyKey, Long referenceId) {
        if (amount <= 0) throw new IllegalArgumentException("Credit amount must be positive");
        if (txRepo.existsByIdempotencyKey(idempotencyKey)) return null; // idempotent skip

        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return save(user, amount, type, description, idempotencyKey, referenceId);
    }

    /**
     * Debit points. Enforces sufficient balance. Idempotent.
     * @throws ApiException 409 if insufficient balance
     */
    @Transactional
    public RewardTransaction debit(Long userId, int amount, RewardTransaction.TransactionType type,
                                   String description, String idempotencyKey, Long referenceId) {
        if (amount <= 0) throw new IllegalArgumentException("Debit amount must be positive");
        if (txRepo.existsByIdempotencyKey(idempotencyKey)) return null;

        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));

        int balance = txRepo.sumBalanceByUserId(userId);
        if (balance < amount) throw ApiException.conflict("Insufficient reward points balance");

        return save(user, -amount, type, description, idempotencyKey, referenceId);
    }

    public Page<RewardTransaction> getHistory(Long userId, Pageable pageable) {
        return txRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    private RewardTransaction save(User user, int amount, RewardTransaction.TransactionType type,
                                   String description, String idempotencyKey, Long referenceId) {
        RewardTransaction tx = new RewardTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setDescription(description);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setReferenceId(referenceId);
        return txRepo.save(tx);
    }
}
