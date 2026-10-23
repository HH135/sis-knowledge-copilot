package io.github.hyun.sis_app_intelligence.ai;

public record ContextChunk(
        String sourceKind,
        String sourceRef,
        String title,
        Integer startLine,
        Integer endLine,
        String content
) {
}
