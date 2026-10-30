package com.learnmate.backend.exam.service;

import com.learnmate.backend.common.exception.ApiException;
import com.learnmate.backend.exam.dto.*;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.repository.*;
import com.learnmate.backend.progress.entity.LearningActivity;
import com.learnmate.backend.progress.entity.UserProgress;
import com.learnmate.backend.progress.service.ProgressService;
import com.learnmate.backend.user.entity.User;
import com.learnmate.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class CodeStudioService {

    private static final int MAX_SAMPLE_RUN_CASES = 5;

    private final ProblemRepository problemRepo;
    private final CodeSubmissionRepository submissionRepo;
    private final UserProblemProgressRepository progressRepo;
    private final UserRepository userRepo;
    private final CodeExecutionService execution;
    private final CodeStudioRateLimiter rateLimiter;
    private final ProgressService progressService;

    public CodeStudioService(ProblemRepository problemRepo,
                             CodeSubmissionRepository submissionRepo,
                             UserProblemProgressRepository progressRepo,
                             UserRepository userRepo,
                             CodeExecutionService execution,
                             CodeStudioRateLimiter rateLimiter,
                             ProgressService progressService) {
        this.problemRepo = problemRepo;
        this.submissionRepo = submissionRepo;
        this.progressRepo = progressRepo;
        this.userRepo = userRepo;
        this.execution = execution;
        this.rateLimiter = rateLimiter;
        this.progressService = progressService;
    }

    // ── Catalogue ────────────────────────────────────────────────────────────

    public Page<Problem> listProblems(Long userId, String difficulty, String topic, String q,
                                      String status, String language, Pageable pageable) {
        Problem.Difficulty diff = parseEnum(Problem.Difficulty.class, difficulty, "difficulty");
        CodeSubmission.Language lang = parseEnum(CodeSubmission.Language.class, language, "language");
        String progressFilter = status == null || status.isBlank() ? "ALL" : status.toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "SOLVED", "ATTEMPTED", "UNSOLVED").contains(progressFilter))
            throw ApiException.badRequest("Invalid status filter");

        Specification<Problem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (diff != null) predicates.add(cb.equal(root.get("difficulty"), diff));
            if (topic != null && !topic.isBlank())
                predicates.add(cb.like(cb.lower(root.get("topic").as(String.class)),
                        "%" + topic.toLowerCase(Locale.ROOT) + "%"));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("tags").as(String.class), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("topic").as(String.class), "")), like)));
            }
            if ("SOLVED".equals(progressFilter)) {
                List<Long> ids = progressRepo.findSolvedProblemIds(userId);
                if (ids.isEmpty()) return cb.disjunction();
                predicates.add(root.get("id").in(ids));
            } else if ("ATTEMPTED".equals(progressFilter)) {
                List<Long> ids = progressRepo.findAttemptedProblemIds(userId);
                if (ids.isEmpty()) return cb.disjunction();
                predicates.add(root.get("id").in(ids));
            } else if ("UNSOLVED".equals(progressFilter)) {
                List<Long> attempted = progressRepo.findAttemptedProblemIds(userId);
                if (!attempted.isEmpty()) predicates.add(cb.not(root.get("id").in(attempted)));
            }
            if (lang != null) {
                List<Long> ids = submissionRepo.findProblemIdsByUserAndLanguage(userId, lang);
                if (ids.isEmpty()) return cb.disjunction();
                predicates.add(root.get("id").in(ids));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return problemRepo.findAll(spec, pageable);
    }

    public Problem getProblem(Long problemId) {
        return problemRepo.findById(problemId)
                .filter(Problem::isActive)
                .orElseThrow(() -> ApiException.notFound("Problem not found"));
    }

    /** Sample test cases only — hidden cases are never serialized. */
    public List<TestCase> getSampleTestCases(Long problemId) {
        return getProblem(problemId).getTestCases().stream()
                .filter(TestCase::isSample)
                .sorted(Comparator.comparingInt(TestCase::getOrderIndex))
                .toList();
    }

    public Map<String, String> getStarterCodes(Long problemId) {
        Problem problem = getProblem(problemId);
        Map<String, String> codes = new LinkedHashMap<>();
        for (CodeSubmission.Language language : CodeSubmission.Language.values()) {
            problem.getStarterCodes().stream()
                    .filter(s -> s.getLanguage() == language)
                    .findFirst()
                    .ifPresent(s -> codes.put(language.name(), s.getCode()));
        }
        return codes;
    }

    // ── Execution ────────────────────────────────────────────────────────────

    public RunResultDto run(Long userId, Long problemId, CodeSubmission.Language language, String code) {
        rateLimiter.check(userId);
        validateSubmission(language, code);
        Problem problem = getProblem(problemId);

        List<TestCase> samples = problem.getTestCases().stream()
                .filter(TestCase::isSample)
                .sorted(Comparator.comparingInt(TestCase::getOrderIndex))
                .limit(MAX_SAMPLE_RUN_CASES)
                .toList();
        if (samples.isEmpty()) throw ApiException.badRequest("This problem has no sample test cases");

        CodeExecutionService.ExecutionOutcome outcome = execution.execute(
                samples, language, code, problem.getTimeLimitMs(), problem.getMemoryLimitMb());

        int passed = (int) outcome.cases().stream().filter(CodeExecutionService.CaseOutcome::passed).count();
        return new RunResultDto(outcome.verdict().name(), passed, samples.size(),
                outcome.timeMs(), outcome.memoryKb(), outcome.compileOutput(),
                outcome.cases().stream().map(this::toCaseDto).toList());
    }

    @Transactional
    public SubmissionDto submit(Long userId, Long problemId, CodeSubmission.Language language, String code) {
        rateLimiter.check(userId);
        validateSubmission(language, code);
        User user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Problem problem = getProblem(problemId);

        List<TestCase> cases = problem.getTestCases().stream()
                .sorted(Comparator.comparingInt(TestCase::getOrderIndex))
                .toList();
        if (cases.isEmpty()) throw ApiException.serviceUnavailable("This problem has no test cases configured");

        CodeExecutionService.ExecutionOutcome outcome = execution.execute(
                cases, language, code, problem.getTimeLimitMs(), problem.getMemoryLimitMb());

        CodeSubmission submission = new CodeSubmission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(language);
        submission.setCode(code);
        submission.setStatus(outcome.verdict());
        submission.setPassedTests((int) outcome.cases().stream().filter(CodeExecutionService.CaseOutcome::passed).count());
        submission.setTotalTests(cases.size());
        submission.setExecutionTimeMs(outcome.timeMs());
        submission.setMemoryUsedKb(outcome.memoryKb());
        submission.setJudgedAt(Instant.now());
        submission.setErrorMessage(errorMessage(outcome));

        for (CodeExecutionService.CaseOutcome result : outcome.cases()) {
            CodeSubmissionCase row = new CodeSubmissionCase();
            row.setSubmission(submission);
            row.setCaseIndex(result.index());
            row.setSample(result.sample());
            row.setPassed(result.passed());
            row.setStatus(result.status());
            row.setExecutionTimeMs(result.timeMs());
            row.setMemoryUsedKb(result.memoryKb());
            if (result.sample()) {
                row.setInput(result.input());
                row.setExpectedOutput(result.expectedOutput());
                row.setActualOutput(result.actualOutput());
                row.setStderr(result.stderr());
            }
            submission.getCaseResults().add(row);
        }

        CodeSubmission saved = submissionRepo.save(submission);
        updateProgress(user, problem, outcome);

        if (saved.getStatus() == CodeSubmission.Status.ACCEPTED) {
            progressService.recordActivity(userId,
                    LearningActivity.ActivityType.CODE_SUBMISSION_ACCEPTED, saved.getId(), 20);
        }
        return toDto(saved);
    }

    private String errorMessage(CodeExecutionService.ExecutionOutcome outcome) {
        if (outcome.compileOutput() != null && !outcome.compileOutput().isBlank())
            return outcome.compileOutput();
        return outcome.cases().stream()
                .filter(c -> c.sample() && c.stderr() != null && !c.stderr().isBlank())
                .map(CodeExecutionService.CaseOutcome::stderr)
                .findFirst()
                .orElse(null);
    }

    private void updateProgress(User user, Problem problem, CodeExecutionService.ExecutionOutcome outcome) {
        UserProblemProgress progress = progressRepo.findByUserIdAndProblemId(user.getId(), problem.getId())
                .orElseGet(() -> {
                    UserProblemProgress created = new UserProblemProgress();
                    created.setUser(user);
                    created.setProblem(problem);
                    return created;
                });
        progress.setAttempts(progress.getAttempts() + 1);
        progress.setLastAttemptAt(Instant.now());
        if (outcome.verdict() == CodeSubmission.Status.ACCEPTED) {
            progress.setSolved(true);
            progress.setAcceptedCount(progress.getAcceptedCount() + 1);
            if (progress.getFirstSolvedAt() == null) progress.setFirstSolvedAt(Instant.now());
            Integer time = outcome.timeMs();
            if (time != null && (progress.getBestTimeMs() == null || time < progress.getBestTimeMs())) {
                progress.setBestTimeMs(time);
            }
        }
        progressRepo.save(progress);
    }

    private void validateSubmission(CodeSubmission.Language language, String code) {
        if (language == null) throw ApiException.badRequest("Language is required");
        if (code == null || code.isBlank()) throw ApiException.badRequest("Code is required");
        if (code.length() > 65535) throw ApiException.badRequest("Code is too long (maximum 65535 characters)");
    }

    // ── Submissions ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SubmissionDto getSubmission(Long userId, Long submissionId) {
        CodeSubmission submission = submissionRepo.findById(submissionId)
                .orElseThrow(() -> ApiException.notFound("Submission not found"));
        if (!submission.getUser().getId().equals(userId)) throw ApiException.forbidden("Access denied");
        return toDto(submission);
    }

    public Page<SubmissionSummaryDto> getHistory(Long userId, Long problemId, String status, String language,
                                                 Pageable pageable) {
        CodeSubmission.Status statusFilter = parseEnum(CodeSubmission.Status.class, status, "status");
        CodeSubmission.Language languageFilter = parseEnum(CodeSubmission.Language.class, language, "language");

        Specification<CodeSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (problemId != null) predicates.add(cb.equal(root.get("problem").get("id"), problemId));
            if (statusFilter != null) predicates.add(cb.equal(root.get("status"), statusFilter));
            if (languageFilter != null) predicates.add(cb.equal(root.get("language"), languageFilter));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return submissionRepo.findAll(spec, pageable).map(this::toSummary);
    }

    // ── Progress ─────────────────────────────────────────────────────────────

    public CodeProgressDto getProgress(Long userId) {
        long total = problemRepo.countByActiveTrue();
        long attempted = progressRepo.countByUserId(userId);
        long solved = progressRepo.countByUserIdAndSolvedTrue(userId);
        long easy = progressRepo.countByUserIdAndSolvedTrueAndProblemDifficulty(userId, Problem.Difficulty.EASY);
        long medium = progressRepo.countByUserIdAndSolvedTrueAndProblemDifficulty(userId, Problem.Difficulty.MEDIUM);
        long hard = progressRepo.countByUserIdAndSolvedTrueAndProblemDifficulty(userId, Problem.Difficulty.HARD);

        long totalSubmissions = submissionRepo.countByUserId(userId);
        long accepted = submissionRepo.countByUserIdAndStatus(userId, CodeSubmission.Status.ACCEPTED);
        double acceptanceRate = totalSubmissions == 0 ? 0.0
                : Math.round(accepted * 1000.0 / totalSubmissions) / 10.0;

        Map<String, Long> totalsByTopic = new LinkedHashMap<>();
        for (Object[] row : problemRepo.countActiveByTopic()) {
            totalsByTopic.put((String) row[0], ((Number) row[1]).longValue());
        }
        Map<String, Long> solvedByTopic = new HashMap<>();
        for (Object[] row : progressRepo.countSolvedByTopic(userId)) {
            solvedByTopic.put((String) row[0], ((Number) row[1]).longValue());
        }
        List<CodeProgressDto.TopicProgress> topics = new ArrayList<>();
        for (Map.Entry<String, Long> entry : totalsByTopic.entrySet()) {
            topics.add(new CodeProgressDto.TopicProgress(entry.getKey(), entry.getValue(),
                    solvedByTopic.getOrDefault(entry.getKey(), 0L)));
        }
        topics.sort(Comparator.comparing(CodeProgressDto.TopicProgress::topic));

        UserProgress userProgress = progressService.getProgress(userId);
        List<SubmissionSummaryDto> recent = submissionRepo.findTop5ByUserIdOrderBySubmittedAtDesc(userId)
                .stream().map(this::toSummary).toList();

        return new CodeProgressDto(total, attempted, solved,
                new CodeProgressDto.DifficultyStats(easy, medium, hard),
                acceptanceRate, totalSubmissions, accepted,
                userProgress.getCurrentStreak(), userProgress.getLongestStreak(),
                userProgress.getTotalXp(), userProgress.getLevel(),
                topics, progressRepo.findSolvedProblemIds(userId), recent);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private SubmissionDto toDto(CodeSubmission submission) {
        return new SubmissionDto(
                submission.getId(),
                submission.getProblem().getId(),
                submission.getProblem().getTitle(),
                submission.getLanguage().name(),
                submission.getCode(),
                submission.getStatus().name(),
                submission.getPassedTests() != null ? submission.getPassedTests() : 0,
                submission.getTotalTests() != null ? submission.getTotalTests() : 0,
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedKb(),
                submission.getErrorMessage(),
                submission.getSubmittedAt(),
                submission.getJudgedAt(),
                submission.getCaseResults().stream()
                        .sorted(Comparator.comparingInt(CodeSubmissionCase::getCaseIndex))
                        .map(this::toCaseDto)
                        .toList());
    }

    private SubmissionSummaryDto toSummary(CodeSubmission submission) {
        return new SubmissionSummaryDto(
                submission.getId(),
                submission.getProblem().getId(),
                submission.getProblem().getTitle(),
                submission.getLanguage().name(),
                submission.getStatus().name(),
                submission.getPassedTests(),
                submission.getTotalTests(),
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedKb(),
                submission.getSubmittedAt(),
                submission.getJudgedAt());
    }

    private SubmissionCaseDto toCaseDto(CodeExecutionService.CaseOutcome result) {
        return new SubmissionCaseDto(
                result.index(),
                result.sample(),
                result.passed(),
                result.status().name(),
                result.timeMs(),
                result.memoryKb(),
                result.sample() ? result.input() : null,
                result.sample() ? result.expectedOutput() : null,
                result.sample() ? result.actualOutput() : null,
                result.sample() ? result.stderr() : null);
    }

    private SubmissionCaseDto toCaseDto(CodeSubmissionCase row) {
        return new SubmissionCaseDto(
                row.getCaseIndex(),
                row.isSample(),
                row.isPassed(),
                row.getStatus() != null ? row.getStatus().name() : null,
                row.getExecutionTimeMs(),
                row.getMemoryUsedKb(),
                row.getInput(),
                row.getExpectedOutput(),
                row.getActualOutput(),
                row.getStderr());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid " + field);
        }
    }
}
