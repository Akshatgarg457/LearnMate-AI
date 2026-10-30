package com.learnmate.backend.auth;

import com.learnmate.backend.auth.dto.LoginRequest;
import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.common.exception.ApiException;
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
class AuthServiceTest {

    @Autowired AuthService authService;

    @Test
    void registerAndLogin_success() {
        var reg = authService.register(new RegisterRequest("Alice", "alice@example.com", "password123"));
        assertThat(reg.accessToken()).isNotBlank();
        assertThat(reg.email()).isEqualTo("alice@example.com");
        assertThat(reg.role()).isEqualTo("STUDENT");

        var login = authService.login(new LoginRequest("alice@example.com", "password123"));
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.userId()).isEqualTo(reg.userId());
    }

    @Test
    void register_duplicateEmail_throws409() {
        authService.register(new RegisterRequest("Bob", "bob@example.com", "password123"));
        assertThatThrownBy(() -> authService.register(new RegisterRequest("Bob2", "bob@example.com", "password456")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void login_wrongPassword_throws401() {
        authService.register(new RegisterRequest("Carol", "carol@example.com", "password123"));
        assertThatThrownBy(() -> authService.login(new LoginRequest("carol@example.com", "wrongpassword")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void register_emailNormalizedToLowercase() {
        var reg = authService.register(new RegisterRequest("Dave", "DAVE@EXAMPLE.COM", "password123"));
        assertThat(reg.email()).isEqualTo("dave@example.com");
    }
}
