package io.github.hyun.sis_app_intelligence.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@SpringBootTest(properties = "app.ai.gemini.request-interval-ms=0")
@ActiveProfiles("portfolio")
class EmbeddingIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    @BeforeEach
    void insertPortfolioChunks() {
        jdbcTemplate.update("DELETE FROM document_chunk WHERE origin = 'portfolio'");
        insertChunk("service", "Repair service workflow", "The repair service assigns work to technicians.");
        insertChunk("inventory", "Parts inventory", "Inventory records part quantities and stock movements.");
        insertChunk("payment", "Payment processing", "Payment records settle completed repair orders.");
    }

    @AfterEach
    void removePortfolioChunksCreatedByTest() {
        jdbcTemplate.update("DELETE FROM document_chunk WHERE origin = 'portfolio'");
    }

    @Test
    void rebuildsMockVectorsThenSkipsUnchangedAndSupportsSimilaritySearch() throws Exception {
        EmbeddingResult first = embeddingService.rebuild(false);
        EmbeddingResult second = embeddingService.rebuild(false);

        assertEquals(3, first.targetChunks());
        assertEquals(3, first.embedded());
        assertEquals(0, first.skippedUnchanged());
        assertEquals(3, second.targetChunks());
        assertEquals(0, second.embedded());
        assertEquals(3, second.skippedUnchanged());
        assertEquals(3, jdbcTemplate.queryForObject("SELECT count(*) FROM chunk_embedding", Integer.class));

        var results = embeddingService.findSimilar("repair service workflow", 5);
        assertEquals(3, results.size());
        assertNotNull(results.getFirst().title());
        assertTrue(results.getFirst().content().length() <= 200);

        MockMvc mockMvc = standaloneSetup(new EmbeddingController(embeddingService)).build();
        mockMvc.perform(post("/api/embeddings/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedded").value(0))
                .andExpect(jsonPath("$.skippedUnchanged").value(3));
        mockMvc.perform(get("/api/embeddings/similar").param("q", "repair service").param("k", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].title").isNotEmpty());
    }

    private void insertChunk(String sourceKind, String title, String content) {
        jdbcTemplate.update("""
                INSERT INTO document_chunk
                    (origin, source_kind, source_ref, title, start_line, end_line, content)
                VALUES ('portfolio', ?, ?, ?, 1, 4, ?)
                """, sourceKind, "portfolio/" + sourceKind, title, content);
    }
}
