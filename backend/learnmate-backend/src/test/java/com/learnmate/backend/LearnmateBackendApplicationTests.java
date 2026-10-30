package com.learnmate.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LearnmateBackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts cleanly with H2 test profile
    }
}
