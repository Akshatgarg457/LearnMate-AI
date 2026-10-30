package com.learnmate.backend.exam.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.learnmate.backend.exam.entity.*;
import com.learnmate.backend.exam.repository.ProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;

/**
 * Loads the original problem catalogue from classpath JSON. Safe to run on every
 * start: problems are updated in place by slug and their cases/starters replaced.
 */
@Component
public class ProblemSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemSeeder.class);

    private final ProblemRepository problemRepo;
    private final boolean enabled;
    private final Resource catalogue;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProblemSeeder(ProblemRepository problemRepo,
                         @Value("${app.code-studio.seed:true}") boolean enabled,
                         @Value("classpath:code-problems.json") Resource catalogue) {
        this.problemRepo = problemRepo;
        this.enabled = enabled;
        this.catalogue = catalogue;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            log.info("Code Studio seed disabled");
            return;
        }
        if (!catalogue.exists()) {
            log.warn("Code Studio catalogue {} not found — skipping seed", catalogue);
            return;
        }

        JsonNode root;
        try (InputStream in = catalogue.getInputStream()) {
            root = mapper.readTree(in);
        }

        int created = 0;
        int updated = 0;
        for (JsonNode node : root.path("problems")) {
            String slug = node.path("slug").asText("").trim();
            if (slug.isEmpty()) continue;

            Problem problem = problemRepo.findBySlug(slug).orElse(null);
            boolean isNew = problem == null;
            if (isNew) {
                problem = new Problem();
                problem.setSlug(slug);
            } else {
                // Flush the orphan deletions before inserting the replacement rows,
                // otherwise the unique (problem, language) key blocks the inserts.
                problem.getTestCases().clear();
                problem.getStarterCodes().clear();
                problemRepo.saveAndFlush(problem);
            }

            problem.setTitle(text(node, "title"));
            problem.setDescription(text(node, "description"));
            problem.setDifficulty(Problem.Difficulty.valueOf(text(node, "difficulty")));
            problem.setTopic(text(node, "topic"));
            problem.setTags(text(node, "tags"));
            problem.setInputFormat(text(node, "inputFormat"));
            problem.setOutputFormat(text(node, "outputFormat"));
            problem.setConstraints(text(node, "constraints"));
            problem.setTimeLimitMs(Math.max(500, node.path("timeLimitMs").asInt(2000)));
            problem.setMemoryLimitMb(Math.max(16, node.path("memoryLimitMb").asInt(256)));
            problem.setActive(true);

            for (JsonNode tcNode : node.path("testCases")) {
                TestCase tc = new TestCase();
                tc.setProblem(problem);
                tc.setInput(tcNode.path("input").asText(""));
                tc.setExpectedOutput(tcNode.path("expectedOutput").asText(""));
                tc.setSample(tcNode.path("sample").asBoolean(false));
                tc.setExplanation(tcNode.path("explanation").isNull() ? null : tcNode.path("explanation").asText(null));
                tc.setOrderIndex(tcNode.path("orderIndex").asInt(0));
                problem.getTestCases().add(tc);
            }

            var starterFields = node.path("starters").properties();
            for (Map.Entry<String, JsonNode> starter : starterFields) {
                try {
                    CodeSubmission.Language language = CodeSubmission.Language.valueOf(starter.getKey());
                    problem.getStarterCodes().add(
                            new ProblemStarterCode(problem, language, starter.getValue().asText()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Unknown starter language {} for problem {}", starter.getKey(), slug);
                }
            }

            problemRepo.save(problem);
            if (isNew) created++; else updated++;
        }
        log.info("Code Studio catalogue ready: {} created, {} updated", created, updated);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
