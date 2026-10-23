package io.github.hyun.sis_app_intelligence.collector;

import java.util.Map;

public record ScanResult(
        String origin,
        int scanned,
        int inserted,
        int updated,
        int unchanged,
        int deleted,
        int skipped,
        Map<String, Integer> skippedReasons,
        Map<String, Integer> byExtension) {
}
