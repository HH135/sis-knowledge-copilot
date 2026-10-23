package io.github.hyun.sis_app_intelligence.document;

public record SimilarChunk(
        long chunkId,
        String sourceKind,
        String title,
        String sourceRef,
        Integer startLine,
        Integer endLine,
        double score,
        String content
) {
}
