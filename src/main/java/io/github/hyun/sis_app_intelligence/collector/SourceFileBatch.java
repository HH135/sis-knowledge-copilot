package io.github.hyun.sis_app_intelligence.collector;

import java.util.List;
import java.util.Map;

import io.github.hyun.sis_app_intelligence.source.SourceFile;

public record SourceFileBatch(
        String origin,
        List<SourceFile> files,
        Map<String, Integer> skippedReasons,
        Map<String, Integer> byExtension) {
}
