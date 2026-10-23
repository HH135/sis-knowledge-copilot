package io.github.hyun.sis_app_intelligence.document;

public record ChunkDraft(
        String origin,
        String sourceKind,
        String sourceRef,
        String title,
        Integer startLine,
        Integer endLine,
        String content,
        String contentSha256
) {
}
