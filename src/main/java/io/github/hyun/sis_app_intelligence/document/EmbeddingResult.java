package io.github.hyun.sis_app_intelligence.document;

public record EmbeddingResult(
        String model,
        int dimensions,
        int targetChunks,
        int embedded,
        int skippedUnchanged,
        int truncated,
        int failed,
        long elapsedMs,
        String lastError
) {
}
