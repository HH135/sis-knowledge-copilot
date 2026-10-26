package io.github.hyun.sis_app_intelligence.document;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import io.github.hyun.sis_app_intelligence.ai.EmbeddingProviderException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class EmbeddingControllerTest {

    @Test
    void rebuildReturnsEmbeddingResult() throws Exception {
        EmbeddingService service = mock(EmbeddingService.class);
        when(service.rebuild(false)).thenReturn(new EmbeddingResult(
                "mock-token-hash-768", 768, 2, 2, 0, 1, 0, 8, null));
        MockMvc mockMvc = standaloneSetup(new EmbeddingController(service)).build();

        mockMvc.perform(post("/api/embeddings/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("mock-token-hash-768"))
                .andExpect(jsonPath("$.dimensions").value(768))
                .andExpect(jsonPath("$.embedded").value(2));
    }

    @Test
    void similarValidatesParametersAndReturnsCitationsWithoutAnswers() throws Exception {
        EmbeddingService service = mock(EmbeddingService.class);
        when(service.findSimilar("repair", 5)).thenReturn(List.of(
                new SimilarChunk(1, "java", "RepairService", "source/RepairService.java",
                        10, 20, 0.98, "source excerpt")));
        MockMvc mockMvc = standaloneSetup(new EmbeddingController(service)).build();

        mockMvc.perform(get("/api/embeddings/similar").param("q", "repair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].chunkId").value(1))
                .andExpect(jsonPath("$.items[0].score").value(0.98))
                .andExpect(jsonPath("$.items[0].content").value("source excerpt"))
                .andExpect(jsonPath("$.answer").doesNotExist());

        mockMvc.perform(get("/api/embeddings/similar").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QUERY_REQUIRED"));
        mockMvc.perform(get("/api/embeddings/similar").param("q", "x").param("k", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_LIMIT"));
    }

    @Test
    void providerErrorIsSanitized() throws Exception {
        EmbeddingService service = mock(EmbeddingService.class);
        when(service.findSimilar("q", 5)).thenThrow(
                new EmbeddingProviderException("private error details", "HTTP 400 INVALID_ARGUMENT"));
        MockMvc mockMvc = standaloneSetup(new EmbeddingController(service)).build();

        mockMvc.perform(get("/api/embeddings/similar").param("q", "q"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("EMBEDDING_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.lastError").value("HTTP 400 INVALID_ARGUMENT"));
    }

    @Test
    void rebuildExposesOnlySafeLastErrorSummary() throws Exception {
        EmbeddingService service = mock(EmbeddingService.class);
        when(service.rebuild(false)).thenReturn(new EmbeddingResult(
                "gemini-embedding-001", 768, 3, 0, 0, 0, 3, 12, "HTTP 400 INVALID_ARGUMENT"));
        MockMvc mockMvc = standaloneSetup(new EmbeddingController(service)).build();

        mockMvc.perform(post("/api/embeddings/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failed").value(3))
                .andExpect(jsonPath("$.lastError").value("HTTP 400 INVALID_ARGUMENT"));
    }
}
