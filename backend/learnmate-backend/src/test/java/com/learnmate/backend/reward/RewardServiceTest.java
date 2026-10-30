package com.learnmate.backend.reward;

import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.reward.entity.RewardTransaction;
import com.learnmate.backend.reward.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RewardServiceTest {

    @Autowired RewardService rewardService;
    @Autowired AuthService authService;

    private Long userId;

    @BeforeEach
    void setup() {
        userId = authService.register(new RegisterRequest("Reward User", "reward@test.com", "pass12345")).userId();
    }

    @Test
    void credit_increasesBalance() {
        rewardService.credit(userId, 100, RewardTransaction.TransactionType.TEACHING_REWARD,
                "test", "idem-1", null);
        assertThat(rewardService.getBalance(userId)).isEqualTo(100);
    }

    @Test
    void credit_idempotent_sameKeyNotDoubled() {
        rewardService.credit(userId, 100, RewardTransaction.TransactionType.TEACHING_REWARD,
                "test", "idem-dup", null);
        var second = rewardService.credit(userId, 100, RewardTransaction.TransactionType.TEACHING_REWARD,
                "test", "idem-dup", null);
        assertThat(second).isNull(); // idempotent skip
        assertThat(rewardService.getBalance(userId)).isEqualTo(100);
    }

    @Test
    void debit_reducesBalance() {
        rewardService.credit(userId, 200, RewardTransaction.TransactionType.TEACHING_REWARD,
                "seed", "seed-1", null);
        rewardService.debit(userId, 50, RewardTransaction.TransactionType.COURSE_REDEMPTION,
                "buy", "debit-1", null);
        assertThat(rewardService.getBalance(userId)).isEqualTo(150);
    }

    @Test
    void debit_insufficientBalance_throws409() {
        assertThatThrownBy(() ->
                rewardService.debit(userId, 999, RewardTransaction.TransactionType.COURSE_REDEMPTION,
                        "overspend", "debit-2", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void initialBalance_isZero() {
        assertThat(rewardService.getBalance(userId)).isZero();
    }
}
