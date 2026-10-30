package com.learnmate.backend.exam;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the seeded catalogue: every problem must be usable by all four languages. */
class CodeProblemsCatalogueTest {

    @Test
    void catalogueHasCompleteProblemsAcrossTopics() throws Exception {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/code-problems.json")) {
            assertThat(in).isNotNull();
            root = new ObjectMapper().readTree(in);
        }

        JsonNode problems = root.path("problems");
        assertThat(problems.size()).isGreaterThanOrEqualTo(30);

        Set<String> slugs = new HashSet<>();
        Set<String> topics = new HashSet<>();
        for (JsonNode problem : problems) {
            String slug = problem.path("slug").asText();
            assertThat(slug).isNotBlank();
            assertThat(slugs.add(slug)).as("duplicate slug %s", slug).isTrue();

            assertThat(problem.path("title").asText()).isNotBlank();
            assertThat(problem.path("description").asText()).isNotBlank();
            assertThat(problem.path("topic").asText()).isNotBlank();
            assertThat(problem.path("timeLimitMs").asInt()).isGreaterThanOrEqualTo(500);
            assertThat(problem.path("memoryLimitMb").asInt()).isGreaterThanOrEqualTo(16);
            topics.add(problem.path("topic").asText());

            int samples = 0;
            int hidden = 0;
            for (JsonNode tc : problem.path("testCases")) {
                if (tc.path("sample").asBoolean()) {
                    samples++;
                    assertThat(tc.path("explanation").asText()).as("sample explanation for %s", slug).isNotBlank();
                } else {
                    hidden++;
                }
                assertThat(tc.path("input").asText()).isNotBlank();
                assertThat(tc.path("expectedOutput").asText()).isNotBlank();
            }
            assertThat(samples).as("samples for %s", slug).isGreaterThanOrEqualTo(2);
            assertThat(hidden).as("hidden cases for %s", slug).isGreaterThanOrEqualTo(1);

            JsonNode starters = problem.path("starters");
            for (String language : new String[]{"JAVA", "PYTHON", "CPP", "JAVASCRIPT"}) {
                assertThat(starters.path(language).asText()).as("starter %s for %s", language, slug).isNotBlank();
            }
        }

        assertThat(topics).hasSizeGreaterThanOrEqualTo(16);
    }
}
