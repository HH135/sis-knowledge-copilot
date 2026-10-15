package io.github.hyun.sis_app_intelligence.metadata;

public record ProcedureMeta(
        String schemaName,
        String procedureName,
        String description,
        String definition,
        String sourceRef
) {
}
