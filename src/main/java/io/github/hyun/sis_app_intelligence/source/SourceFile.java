package io.github.hyun.sis_app_intelligence.source;

public record SourceFile(
        String origin,
        String relativePath,
        String fileType,
        String charsetName,
        String content,
        String sha256,
        long sizeBytes) {
}
