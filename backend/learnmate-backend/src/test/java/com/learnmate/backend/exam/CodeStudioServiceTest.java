package com.learnmate.backend.exam;

import com.learnmate.backend.auth.dto.RegisterRequest;
import com.learnmate.backend.auth.service.AuthService;
import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.exam.dto.SubmissionCaseDto;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.judge.JudgeProvider;
import com.learnmate.backend.exam.judge.JudgeResult;
import com.learnmate.backend.exam.repository.*;
import com.learnmate.backend.exam.service.CodeStudioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service-level tests with the external judge mocked. They prove persistence,
 * filtering and hidden-case protection, not that a real sandbox executes code.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CodeStudioServiceTest {

    @Autowired CodeStudioService service;
    @Autowired AuthService authService;
    @Autowired ProblemRepository problemRepo;
    @Autowired CodeSubmissionRepository submissionRepo;
    @Autowired UserProblemProgressRepository progressRepo;
    @MockitoBean JudgeProvider judge;

    private Long userId;

    @BeforeEach
    void setup() {
        userId = authService.register(new RegisterRequest("Code User", "code-" + UUID.randomUUID() + "@test.com", "pass12345")).userId();
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(inv -> accepted(sumOf(inv.<String>getArgument(2))));
    }

    @Test
    void runExecutesSamplesOnly() {
        Long problemId = createProblem("demo-run", 2, 2);

        var result = service.run(userId, problemId, CodeSubmission.Language.PYTHON, "print(1)");

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.cases()).hasSize(2).allMatch(SubmissionCaseDto::sample);
        verify(judge, times(2)).execute(anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void submitAcceptedPersistsAndRedactsHiddenCases() {
        Long problemId = createProblem("demo-accepted", 2, 1);

        var dto = service.submit(userId, problemId, CodeSubmission.Language.PYTHON, "print(1)");

        assertThat(dto.status()).isEqualTo("ACCEPTED");
        assertThat(dto.passedTests()).isEqualTo(3);
        assertThat(dto.totalTests()).isEqualTo(3);
        assertThat(dto.cases()).hasSize(3);

        var hidden = dto.cases().stream().filter(c -> !c.sample()).findFirst().orElseThrow();
        assertThat(hidden.input()).isNull();
        assertThat(hidden.expectedOutput()).isNull();
        assertThat(hidden.actualOutput()).isNull();
        assertThat(hidden.stderr()).isNull();

        CodeSubmission saved = submissionRepo.findById(dto.id()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CodeSubmission.Status.ACCEPTED);
        assertThat(saved.getCaseResults()).hasSize(3);
        CodeSubmissionCase hiddenRow = saved.getCaseResults().stream().filter(c -> !c.isSample()).findFirst().orElseThrow();
        assertThat(hiddenRow.getInput()).isNull();
        assertThat(hiddenRow.getExpectedOutput()).isNull();
        assertThat(hiddenRow.getActualOutput()).isNull();

        UserProblemProgress progress = progressRepo.findByUserIdAndProblemId(userId, problemId).orElseThrow();
        assertThat(progress.isSolved()).isTrue();
        assertThat(progress.getAttempts()).isEqualTo(1);
        assertThat(progress.getAcceptedCount()).isEqualTo(1);
        assertThat(progress.getFirstSolvedAt()).isNotNull();
    }

    @Test
    void wrongAnswerStillSavesTheAttempt() {
        Long problemId = createProblem("demo-wrong", 1, 1);
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult("Accepted", "0", "", "", 4, 900, true));

        var dto = service.submit(userId, problemId, CodeSubmission.Language.PYTHON, "print(0)");

        assertThat(dto.status()).isEqualTo("WRONG_ANSWER");
        assertThat(dto.passedTests()).isZero();
        assertThat(submissionRepo.findById(dto.id())).isPresent();

        UserProblemProgress progress = progressRepo.findByUserIdAndProblemId(userId, problemId).orElseThrow();
        assertThat(progress.isSolved()).isFalse();
        assertThat(progress.getAttempts()).isEqualTo(1);
    }

    @Test
    void compilationErrorIsReportedAndNotAccepted() {
        Long problemId = createProblem("demo-ce", 1, 1);
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult("Compilation Error", "", "", "error: ';' expected", null, null, true));

        var dto = service.submit(userId, problemId, CodeSubmission.Language.JAVA, "bad code");

        assertThat(dto.status()).isEqualTo("COMPILATION_ERROR");
        assertThat(dto.status()).isNotEqualTo("ACCEPTED");
        assertThat(dto.errorMessage()).contains("error");
        assertThat(dto.cases()).allMatch(c -> c.status().equals("COMPILATION_ERROR"));
    }

    @Test
    void submissionOwnershipIsEnforced() {
        Long problemId = createProblem("demo-owner", 1, 0);
        var dto = service.submit(userId, problemId, CodeSubmission.Language.PYTHON, "print(1)");
        Long otherUserId = authService.register(
                new RegisterRequest("Other User", "other-" + UUID.randomUUID() + "@test.com", "pass12345")).userId();

        assertThat(service.getSubmission(userId, dto.id()).id()).isEqualTo(dto.id());
        assertThatThrownBy(() -> service.getSubmission(otherUserId, dto.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void historyFiltersByStatusAndLanguage() {
        Long problemId = createProblem("demo-history", 1, 0);
        service.submit(userId, problemId, CodeSubmission.Language.PYTHON, "print(1)");

        var accepted = service.getHistory(userId, problemId, "ACCEPTED", "PYTHON", org.springframework.data.domain.PageRequest.of(0, 10));
        var wrong = service.getHistory(userId, problemId, "WRONG_ANSWER", null, org.springframework.data.domain.PageRequest.of(0, 10));
        var java = service.getHistory(userId, problemId, null, "JAVA", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(accepted.getContent()).hasSize(1);
        assertThat(wrong.getContent()).isEmpty();
        assertThat(java.getContent()).isEmpty();
    }

    @Test
    void progressAggregatesSolvedCounts() {
        Long problemId = createProblem("demo-progress", 1, 1);
        service.submit(userId, problemId, CodeSubmission.Language.PYTHON, "print(1)");

        var progress = service.getProgress(userId);

        assertThat(progress.attempted()).isEqualTo(1L);
        assertThat(progress.solved()).isEqualTo(1L);
        assertThat(progress.solvedByDifficulty().easy()).isEqualTo(1L);
        assertThat(progress.acceptanceRate()).isEqualTo(100.0);
        assertThat(progress.solvedProblemIds()).contains(problemId);
        assertThat(progress.topics()).anyMatch(t -> t.topic().equals("Arrays") && t.solved() == 1);
        assertThat(progress.recentSubmissions()).hasSize(1);
    }

    @Test
    void starterCodesAndSamplesAreExposed() {
        Long problemId = createProblem("demo-starters", 2, 1);

        var starters = service.getStarterCodes(problemId);
        var samples = service.getSampleTestCases(problemId);

        assertThat(starters).containsOnlyKeys("JAVA", "PYTHON", "CPP", "JAVASCRIPT");
        assertThat(samples).hasSize(2).allMatch(TestCase::isSample);
        assertThat(samples).extracting(TestCase::getOrderIndex).containsExactly(0, 1);
    }

    @Test
    void rejectedSubmissionWithInactiveProblemIsNotFound() {
        Long problemId = createProblem("demo-hidden-problem", 1, 0);
        Problem problem = problemRepo.findById(problemId).orElseThrow();
        problem.setActive(false);
        problemRepo.save(problem);

        assertThatThrownBy(() -> service.getProblem(problemId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private Long createProblem(String prefix, int samples, int hidden) {
        Problem problem = new Problem();
        problem.setSlug(prefix + "-" + UUID.randomUUID());
        problem.setTitle("Sum Input");
        problem.setDescription("Add every integer from standard input.");
        problem.setDifficulty(Problem.Difficulty.EASY);
        problem.setTopic("Arrays");
        problem.setTags("arrays,math");
        int order = 0;
        for (int i = 0; i < samples; i++) problem.getTestCases().add(caseFor(problem, "1 2 3", "6", true, order++));
        for (int i = 0; i < hidden; i++) problem.getTestCases().add(caseFor(problem, "40 2", "42", false, order++));
        for (CodeSubmission.Language language : CodeSubmission.Language.values()) {
            problem.getStarterCodes().add(new ProblemStarterCode(problem, language, "// " + language));
        }
        return problemRepo.save(problem).getId();
    }

    private static TestCase caseFor(Problem problem, String input, String expected, boolean sample, int order) {
        TestCase tc = new TestCase();
        tc.setProblem(problem);
        tc.setInput(input);
        tc.setExpectedOutput(expected);
        tc.setSample(sample);
        tc.setOrderIndex(order);
        return tc;
    }

    private static JudgeResult accepted(String stdout) {
        return new JudgeResult("Accepted", stdout, "", "", 9, 2500, true);
    }

    private static String sumOf(String stdin) {
        int total = 0;
        for (String token : stdin.trim().split("\\s+")) {
            if (!token.isBlank()) total += Integer.parseInt(token);
        }
        return Integer.toString(total);
    }
}
