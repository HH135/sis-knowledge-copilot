package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.hyun.sis_app_intelligence.ai.AiAnalysisPort;
import io.github.hyun.sis_app_intelligence.ai.AnalysisProviderException;
import io.github.hyun.sis_app_intelligence.dependency.DependencyService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RagControllerTest {

    @Test
    void validatesQuestionAndReturnsFallbackAndProviderFailureWithoutFailingRequest() throws Exception {
        FtsRetriever fts = mock(FtsRetriever.class);
        VectorRetriever vector = mock(VectorRetriever.class);
        HybridRetriever hybrid = mock(HybridRetriever.class);
        DependencyService dependencies = mock(DependencyService.class);
        RagRepository repository = mock(RagRepository.class);
        AiAnalysisPort ai = mock(AiAnalysisPort.class);
        when(ai.providerName()).thenReturn("mock");
        when(fts.retrieve("internal", "what?")).thenReturn(List.of(
                new RetrievedChunk(10, "FILE", "Sample", "sample.sql", 1, 3, "text",
                        1, null, 0, false, null)));
        when(ai.analyze(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new AnalysisProviderException("unavailable", "HTTP 503 UNAVAILABLE"));

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("internal");
        RagService service = new RagService(
                fts,
                vector,
                hybrid,
                new DependencyExpander(dependencies, repository),
                new CitationValidator(),
                ai,
                environment);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mockMvc = standaloneSetup(new RagController(service))
                .setControllerAdvice(new RagExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(post("/api/rag/ask")
                        .contentType("application/json")
                        .content("{\"question\":\"   \",\"mode\":\"HYBRID\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/rag/ask")
                        .contentType("application/json")
                        .content("{\"question\":\"what?\",\"mode\":\"HYBRID\",\"expand\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("FTS"))
                .andExpect(jsonPath("$.modeFallback").value("FTS"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lastError").value("HTTP 503 UNAVAILABLE"))
                .andExpect(jsonPath("$.retrieval[0].chunkId").value(10))
                .andExpect(jsonPath("$.citations[0].title").value("Sample"));
        verify(fts).retrieve("internal", "what?");
    }

}
