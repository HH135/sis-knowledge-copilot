package io.github.hyun.sis_app_intelligence.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagAskRequest(
        @NotBlank
        @Size(max = 500)
        String question,
        RetrievalMode mode,
        @Min(1)
        @Max(10)
        Integer k,
        Boolean expand
) {
}
