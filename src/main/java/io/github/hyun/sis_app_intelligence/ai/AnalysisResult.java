package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;

public record AnalysisResult(
        String answer,
        List<Citation> citations,
        String provider
) {
    public AnalysisResult {
        citations = List.copyOf(citations);
    }
}
