package io.github.hyun.sis_app_intelligence.rag;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import io.github.hyun.sis_app_intelligence.collector.SourceCollectorService;
import io.github.hyun.sis_app_intelligence.document.ChunkingService;
import io.github.hyun.sis_app_intelligence.document.EmbeddingService;
import io.github.hyun.sis_app_intelligence.metadata.MetadataSyncService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.ai.embedding.provider=mock",
        "app.ai.external-enabled=false",
        "app.ai.gemini.request-interval-ms=0"
})
@ActiveProfiles({"test", "portfolio"})
@Transactional
class RetrievalEvaluationTest {

    private static final String FIXTURE_PATH = "/rag/retrieval-evaluation-v1.json";
    private static final int TOP_K = 5;
    private static final int EXPECTED_QUESTION_COUNT = 20;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private SourceCollectorService sourceCollectorService;

    @Autowired
    private ChunkingService chunkingService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private HybridRetriever hybridRetriever;

    @Test
    void evaluatesPortfolioHybridRetrievalAgainstVersionedQuestions() throws IOException {
        assertTrue(environment.matchesProfiles("test"), "Evaluation requires the test profile.");
        assertTrue(environment.matchesProfiles("portfolio"), "Evaluation requires the portfolio profile.");
        assertFalse(environment.matchesProfiles("internal"), "Evaluation must not use the internal profile.");
        assertEquals("sis_app_test", jdbcTemplate.queryForObject("SELECT current_database()", String.class),
                "Evaluation may only run against sis_app_test.");

        EvaluationFixture fixture = loadFixture();
        validateFixture(fixture);
        metadataSyncService.sync();
        sourceCollectorService.scan();
        chunkingService.rebuild();
        embeddingService.rebuild(true);

        double totalHit = 0;
        double totalRecall = 0;
        for (EvaluationQuestion question : fixture.questions()) {
            List<RetrievedChunk> results = hybridRetriever.retrieve("portfolio", question.query(), TOP_K);
            Set<String> retrievedSources = results.stream()
                    .map(RetrievedChunk::sourceRef)
                    .collect(java.util.stream.Collectors.toSet());
            long relevantRetrieved = question.expectedSources().stream()
                    .filter(retrievedSources::contains)
                    .distinct()
                    .count();
            double hit = relevantRetrieved > 0 ? 1 : 0;
            double recall = relevantRetrieved / (double) question.expectedSources().size();
            totalHit += hit;
            totalRecall += recall;
            System.out.printf(Locale.ROOT, "%s Hit@%d=%.3f Recall@%d=%.3f%n",
                    question.id(), TOP_K, hit, TOP_K, recall);
        }

        System.out.printf(Locale.ROOT, "Macro Hit@%d=%.3f Recall@%d=%.3f questions=%d fixture=%s%n",
                TOP_K,
                totalHit / fixture.questions().size(),
                TOP_K,
                totalRecall / fixture.questions().size(),
                fixture.questions().size(),
                fixture.version());
    }

    private EvaluationFixture loadFixture() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(FIXTURE_PATH)) {
            if (input == null) {
                throw new IOException("Missing retrieval evaluation fixture: " + FIXTURE_PATH);
            }
            return objectMapper.readValue(input, EvaluationFixture.class);
        }
    }

    private void validateFixture(EvaluationFixture fixture) {
        assertFalse(fixture.version().isBlank(), "Fixture version must be set.");
        assertEquals(EXPECTED_QUESTION_COUNT, fixture.questions().size(),
                "The versioned retrieval evaluation fixture must contain exactly 20 questions.");
        Set<String> questionIds = new HashSet<>();
        for (EvaluationQuestion question : fixture.questions()) {
            assertTrue(questionIds.add(question.id()), "Duplicate question id: " + question.id());
            assertFalse(question.query().isBlank(), "Question query must not be blank: " + question.id());
            assertFalse(question.expectedSources().isEmpty(),
                    "Each question must have at least one expected source: " + question.id());
            assertEquals(question.expectedSources().size(), new HashSet<>(question.expectedSources()).size(),
                    "Duplicate expected source: " + question.id());
        }
    }

    private record EvaluationFixture(String version, List<EvaluationQuestion> questions) {
    }

    private record EvaluationQuestion(String id, String query, List<String> expectedSources) {
    }
}
