package io.github.hyun.sis_app_intelligence.document;

import java.util.Map;

public record ChunkResult(String origin, int total, Map<String, Integer> byKind, int mapperFallbacks) {
}
