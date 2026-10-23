package io.github.hyun.sis_app_intelligence.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.github.hyun.sis_app_intelligence.collector.SourceCollectorService;
import io.github.hyun.sis_app_intelligence.dependency.DependencyService;
import io.github.hyun.sis_app_intelligence.document.ChunkingService;
import io.github.hyun.sis_app_intelligence.document.EmbeddingService;
import io.github.hyun.sis_app_intelligence.metadata.MetadataSyncService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.ai.gemini.request-interval-ms=0")
@ActiveProfiles("portfolio")
@Transactional
class RagIntegrationTest {

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private SourceCollectorService sourceCollectorService;

    @Autowired
    private ChunkingService chunkingService;

    @Autowired
    private DependencyService dependencyService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private RagService ragService;

    @Autowired
    private RagRepository ragRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void answersHybridQuestionAndExpandsResolvedProcedureContext() {
        metadataSyncService.sync();
        sourceCollectorService.scan();
        chunkingService.rebuild();
        dependencyService.rebuild();
        embeddingService.rebuild(true);
        assertTrue(ragRepository.searchFts("portfolio", "LETE_REPA", 20).stream()
                .anyMatch(chunk -> chunk.title().contains("USP_COMPLETE_REPAIR_ORDER")));

        var response = ragService.ask(new RagAskRequest(
                "USP_COMPLETE_REPAIR_ORDER", RetrievalMode.HYBRID, 6, true));

        assertEquals("HYBRID", response.get("mode").toString());
        assertEquals("mock", response.get("provider"));
        assertTrue(((java.util.List<?>) response.get("retrieval")).size() <= 10);
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT count(*) > 0 FROM document_chunk WHERE origin = 'portfolio'", Boolean.class));
        assertTrue(((java.util.List<?>) response.get("retrieval")).stream()
                .map(item -> (java.util.Map<?, ?>) item)
                .anyMatch(item -> Boolean.TRUE.equals(item.get("expanded"))));
    }
}
