package io.github.hyun.sis_app_intelligence.document;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ChunkingControllerTest {

    @Test
    void rebuildReturnsKindCounts() throws Exception {
        ChunkingService service = mock(ChunkingService.class);
        when(service.rebuild()).thenReturn(new ChunkResult(
                "portfolio", 52,
                java.util.Map.of("FILE", 0, "MAPPER_STATEMENT", 14, "PROCEDURE", 18, "TABLE", 20),
                0));
        MockMvc mockMvc = standaloneSetup(new ChunkingController(service)).build();

        mockMvc.perform(post("/api/chunks/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("portfolio"))
                .andExpect(jsonPath("$.total").value(52))
                .andExpect(jsonPath("$.byKind.MAPPER_STATEMENT").value(14))
                .andExpect(jsonPath("$.mapperFallbacks").value(0));
    }
}
