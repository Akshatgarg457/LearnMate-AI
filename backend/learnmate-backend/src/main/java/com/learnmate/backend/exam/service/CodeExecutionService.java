package com.learnmate.backend.exam.service;

import com.learnmate.backend.exam.entity.CodeSubmission;
import com.learnmate.backend.exam.entity.TestCase;
import com.learnmate.backend.exam.judge.JudgeProvider;
import com.learnmate.backend.exam.judge.JudgeResult;
import com.learnmate.backend.exam.judge.OutputNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs code against a set of test cases through the configured external judge.
 * Test execution never happens inside this process.
 */
@Service
public class CodeExecutionService {

    private static final int MAX_PARALLEL_CASES = 3;

    private final JudgeProvider judge;
    private final ExecutorService executor;

    public CodeExecutionService(JudgeProvider judge) {
        this.judge = judge;
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(MAX_PARALLEL_CASES, runnable -> {
            Thread thread = new Thread(runnable, "code-judge-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public ExecutionOutcome execute(List<TestCase> cases, CodeSubmission.Language language,
                                    String code, int timeLimitMs, int memoryLimitMb) {
        List<Future<CaseOutcome>> futures = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            TestCase testCase = cases.get(i);
            int index = i;
            futures.add(executor.submit(() -> runCase(index, testCase, language, code, timeLimitMs, memoryLimitMb)));
        }

        List<CaseOutcome> outcomes = new ArrayList<>(cases.size());
        String compileOutput = null;
        for (Future<CaseOutcome> future : futures) {
            CaseOutcome outcome;
            try {
                outcome = future.get(90, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                outcome = new CaseOutcome(outcomes.size(), false, false, CodeSubmission.Status.JUDGE_ERROR,
                        null, null, null, null, null, "The judge did not respond in time.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the code judge", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("Code judge call failed", cause);
            }
            if (outcome.status() == CodeSubmission.Status.COMPILATION_ERROR && compileOutput == null) {
                compileOutput = outcome.stderr();
            }
            outcomes.add(outcome);
        }

        CodeSubmission.Status verdict = aggregate(outcomes);
        Integer maxTime = outcomes.stream().map(CaseOutcome::timeMs).filter(t -> t != null).max(Integer::compareTo).orElse(null);
        Integer maxMemory = outcomes.stream().map(CaseOutcome::memoryKb).filter(m -> m != null).max(Integer::compareTo).orElse(null);
        return new ExecutionOutcome(verdict, outcomes, maxTime, maxMemory, compileOutput);
    }

    private CaseOutcome runCase(int index, TestCase testCase, CodeSubmission.Language language,
                                String code, int timeLimitMs, int memoryLimitMb) {
        JudgeResult result = judge.execute(language.name(), code, testCase.getInput(), timeLimitMs, memoryLimitMb);
        CodeSubmission.Status status = mapStatus(result.status());
        boolean accepted = status == CodeSubmission.Status.ACCEPTED;
        boolean passed = accepted && OutputNormalizer.matches(result.stdout(), testCase.getExpectedOutput());

        String stderr = result.compileOutput() != null && !result.compileOutput().isBlank()
                ? result.compileOutput()
                : result.stderr();

        if (passed) status = CodeSubmission.Status.ACCEPTED;
        else if (accepted) status = CodeSubmission.Status.WRONG_ANSWER;

        boolean visible = testCase.isSample();
        return new CaseOutcome(
                index,
                visible,
                passed,
                status,
                result.timeMs(),
                result.memoryKb(),
                visible ? testCase.getInput() : null,
                visible ? testCase.getExpectedOutput() : null,
                visible ? result.stdout() : null,
                visible ? stderr : (status == CodeSubmission.Status.COMPILATION_ERROR ? stderr : null)
        );
    }

    static CodeSubmission.Status mapStatus(String judgeStatus) {
        if (judgeStatus == null) return CodeSubmission.Status.JUDGE_ERROR;
        String status = judgeStatus.toLowerCase();
        if (status.contains("accepted")) return CodeSubmission.Status.ACCEPTED;
        if (status.contains("wrong answer")) return CodeSubmission.Status.WRONG_ANSWER;
        if (status.contains("time limit")) return CodeSubmission.Status.TIME_LIMIT_EXCEEDED;
        if (status.contains("memory limit")) return CodeSubmission.Status.MEMORY_LIMIT_EXCEEDED;
        if (status.contains("compilation error")) return CodeSubmission.Status.COMPILATION_ERROR;
        if (status.contains("runtime error")) return CodeSubmission.Status.RUNTIME_ERROR;
        return CodeSubmission.Status.JUDGE_ERROR;
    }

    private static CodeSubmission.Status aggregate(List<CaseOutcome> outcomes) {
        CodeSubmission.Status worst = CodeSubmission.Status.ACCEPTED;
        for (CaseOutcome outcome : outcomes) {
            if (severity(outcome.status()) > severity(worst)) worst = outcome.status();
        }
        return worst;
    }

    private static int severity(CodeSubmission.Status status) {
        return switch (status) {
            case ACCEPTED -> 0;
            case WRONG_ANSWER -> 1;
            case RUNTIME_ERROR -> 2;
            case MEMORY_LIMIT_EXCEEDED -> 3;
            case TIME_LIMIT_EXCEEDED -> 4;
            case COMPILATION_ERROR -> 5;
            case JUDGE_ERROR -> 6;
            default -> 7;
        };
    }

    public record CaseOutcome(
            int index,
            boolean sample,
            boolean passed,
            CodeSubmission.Status status,
            Integer timeMs,
            Integer memoryKb,
            String input,
            String expectedOutput,
            String actualOutput,
            String stderr
    ) {}

    public record ExecutionOutcome(
            CodeSubmission.Status verdict,
            List<CaseOutcome> cases,
            Integer timeMs,
            Integer memoryKb,
            String compileOutput
    ) {}
}
