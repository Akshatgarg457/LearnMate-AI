package com.learnmate.backend.exam;

import com.learnmate.backend.exam.entity.CodeSubmission;
import com.learnmate.backend.exam.entity.TestCase;
import com.learnmate.backend.exam.judge.JudgeProvider;
import com.learnmate.backend.exam.judge.JudgeResult;
import com.learnmate.backend.exam.service.CodeExecutionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for verdict mapping, output comparison and hidden-case redaction.
 * The external sandbox is replaced by a Mockito stub: these are mocked-judge tests,
 * not evidence that a real execution provider works.
 */
class CodeExecutionServiceTest {

    private final JudgeProvider judge = mock(JudgeProvider.class);
    private final CodeExecutionService service = new CodeExecutionService(judge);

    @Test
    void acceptsWhenEveryOutputMatches() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(inv -> accepted(sumOf(inv.<String>getArgument(2))));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0), testCase("10 20", "30", true, 1)),
                CodeSubmission.Language.PYTHON, "print(sum(...))", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.ACCEPTED);
        assertThat(outcome.cases()).hasSize(2).allMatch(CodeExecutionService.CaseOutcome::passed);
        assertThat(outcome.timeMs()).isEqualTo(12);
        assertThat(outcome.memoryKb()).isEqualTo(3000);
    }

    @Test
    void normalizesWhitespaceBeforeComparing() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult("Accepted", "6 \r\n\n", "", "", 5, 1000, true));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0)),
                CodeSubmission.Language.PYTHON, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.ACCEPTED);
        assertThat(outcome.cases().getFirst().passed()).isTrue();
    }

    @Test
    void wrongOutputIsWrongAnswerNotAccepted() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult("Accepted", "99", "", "", 5, 1000, true));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0)),
                CodeSubmission.Language.PYTHON, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.WRONG_ANSWER);
        assertThat(outcome.cases().getFirst().passed()).isFalse();
        assertThat(outcome.cases().getFirst().actualOutput()).isEqualTo("99");
    }

    @Test
    void hiddenCaseDataIsRemovedFromOutcome() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(inv -> {
                    String stdin = inv.getArgument(2);
                    return stdin.contains("7 8")
                            ? new JudgeResult("Wrong Answer", "0", "", "", 5, 1000, true)
                            : accepted(sumOf(stdin));
                });

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0), testCase("7 8", "15", false, 1)),
                CodeSubmission.Language.JAVA, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.WRONG_ANSWER);
        assertThat(outcome.cases().get(0).sample()).isTrue();
        assertThat(outcome.cases().get(0).input()).isEqualTo("1 2 3");

        var hidden = outcome.cases().get(1);
        assertThat(hidden.sample()).isFalse();
        assertThat(hidden.passed()).isFalse();
        assertThat(hidden.input()).isNull();
        assertThat(hidden.expectedOutput()).isNull();
        assertThat(hidden.actualOutput()).isNull();
        assertThat(hidden.stderr()).isNull();
    }

    @Test
    void compilationErrorIsReportedWithoutHiddenData() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult("Compilation Error", "", "", "Main.java:1: error: ';' expected", null, null, true));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0), testCase("7 8", "15", false, 1)),
                CodeSubmission.Language.JAVA, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.COMPILATION_ERROR);
        assertThat(outcome.compileOutput()).contains("error");
        assertThat(outcome.cases()).allMatch(c -> c.status() == CodeSubmission.Status.COMPILATION_ERROR && !c.passed());

        var hidden = outcome.cases().get(1);
        assertThat(hidden.input()).isNull();
        assertThat(hidden.expectedOutput()).isNull();
        assertThat(hidden.stderr()).contains("error");
    }

    @Test
    void timeLimitExceededIsNotAccepted() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult("Time Limit Exceeded", null, null, null, 2000, 2000, true));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0)),
                CodeSubmission.Language.CPP, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.TIME_LIMIT_EXCEEDED);
        assertThat(outcome.cases().getFirst().passed()).isFalse();
    }

    @Test
    void severityPicksTheWorstCaseVerdict() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(inv -> inv.<String>getArgument(2).contains("7 8")
                        ? new JudgeResult("Runtime Error", "", "boom", "", 5, 1000, true)
                        : accepted(sumOf(inv.getArgument(2))));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0), testCase("7 8", "15", false, 1)),
                CodeSubmission.Language.JAVASCRIPT, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.RUNTIME_ERROR);
        assertThat(outcome.cases().stream().filter(CodeExecutionService.CaseOutcome::passed)).hasSize(1);
    }

    @Test
    void unknownJudgeStatusBecomesJudgeError() {
        when(judge.execute(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new JudgeResult(null, null, null, null, null, null, true));

        var outcome = service.execute(
                List.of(testCase("1 2 3", "6", true, 0)),
                CodeSubmission.Language.PYTHON, "code", 2000, 256);

        assertThat(outcome.verdict()).isEqualTo(CodeSubmission.Status.JUDGE_ERROR);
        assertThat(outcome.verdict()).isNotEqualTo(CodeSubmission.Status.ACCEPTED);
    }

    private static TestCase testCase(String input, String expected, boolean sample, int order) {
        TestCase tc = new TestCase();
        tc.setInput(input);
        tc.setExpectedOutput(expected);
        tc.setSample(sample);
        tc.setOrderIndex(order);
        return tc;
    }

    private static JudgeResult accepted(String stdout) {
        return new JudgeResult("Accepted", stdout, "", "", 12, 3000, true);
    }

    private static String sumOf(String stdin) {
        int total = 0;
        for (String token : stdin.trim().split("\\s+")) {
            if (!token.isBlank()) total += Integer.parseInt(token);
        }
        return Integer.toString(total);
    }
}
