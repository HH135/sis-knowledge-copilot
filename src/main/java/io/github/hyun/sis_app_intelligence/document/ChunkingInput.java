package io.github.hyun.sis_app_intelligence.document;

public record ChunkingInput(
        String origin,
        String sourceKind,
        String sourceRef,
        String title,
        String content,
        String description,
        String definitionStatus
) {
}
