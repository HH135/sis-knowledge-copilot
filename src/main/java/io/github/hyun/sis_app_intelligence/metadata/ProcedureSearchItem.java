package io.github.hyun.sis_app_intelligence.metadata;

public record ProcedureSearchItem(
        String databaseName,
        String schemaName,
        String procedureName,
        String description,
        String sourceRef,
        String matchedIn
) {
}
