package io.github.hyun.sis_app_intelligence.rag;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalEvaluationFixtureTest {

    private static final String FIXTURE_PATH = "/rag/retrieval-evaluation-v1.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void containsTwentyVersionedQuestionsWithExistingPortfolioSources() throws Exception {
        JsonNode fixture;
        try (InputStream input = getClass().getResourceAsStream(FIXTURE_PATH)) {
            assertTrue(input != null, "Missing fixture: " + FIXTURE_PATH);
            fixture = objectMapper.readTree(input);
        }

        assertEquals("1.0.0", fixture.path("version").asString());
        JsonNode questions = fixture.path("questions");
        assertEquals(20, questions.size());

        JsonNode metadata;
        try (InputStream input = getClass().getResourceAsStream(
                "/portfolio-data/metadata/tables.json")) {
            assertTrue(input != null, "Missing portfolio table metadata fixture.");
            metadata = objectMapper.readTree(input);
        }
        Set<String> tableSources = new HashSet<>();
        metadata.path("tables").forEach(table ->
                tableSources.add(table.path("schema").asString() + "." + table.path("name").asString()));

        Set<String> ids = new HashSet<>();
        for (JsonNode question : questions) {
            String id = question.path("id").asString();
            assertFalse(id.isBlank(), "Question id must not be blank.");
            assertTrue(ids.add(id), "Duplicate question id: " + id);
            assertFalse(question.path("query").asString().isBlank(), "Blank query: " + id);
            JsonNode expectedSources = question.path("expectedSources");
            assertTrue(expectedSources.isArray() && !expectedSources.isEmpty(),
                    "Expected sources are missing: " + id);
            for (JsonNode expectedSource : expectedSources) {
                String sourceRef = expectedSource.asString();
                if (sourceRef.startsWith("portfolio-data/")) {
                    String resourcePath = sourceRef.split("#", 2)[0];
                    assertTrue(getClass().getClassLoader().getResource(resourcePath) != null,
                            "Expected portfolio source does not exist: " + sourceRef);
                }
                else {
                    assertTrue(tableSources.contains(sourceRef),
                            "Expected table source does not exist: " + sourceRef);
                }
            }
        }
    }
}
