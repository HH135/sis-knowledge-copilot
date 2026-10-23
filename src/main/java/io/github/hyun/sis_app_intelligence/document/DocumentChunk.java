package io.github.hyun.sis_app_intelligence.document;

public record DocumentChunk(
        long id,
        String sourceKind,
        String sourceRef,
        String title,
        Integer startLine,
        Integer endLine,
        String content,
        String contentSha256,
        String embeddingModel
) {
}
