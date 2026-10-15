package io.github.hyun.sis_app_intelligence.metadata;

public record SyncResult(
        String databaseName,
        int tableCount,
        int columnCount,
        int procedureCount
) {
}
