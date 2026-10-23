package io.github.hyun.sis_app_intelligence.dependency;

import java.util.Map;

public record DependencyRebuildResult(
        String origin,
        int edges,
        Map<String, Integer> byType,
        int mapperEdges
) {
}
