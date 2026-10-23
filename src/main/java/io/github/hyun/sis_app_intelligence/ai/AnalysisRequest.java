package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
        @NotBlank
        @Size(max = 500)
        String question,
        @Size(max = 10)
        List<ContextChunk> contexts
) {
    public AnalysisRequest {
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
    }
}
