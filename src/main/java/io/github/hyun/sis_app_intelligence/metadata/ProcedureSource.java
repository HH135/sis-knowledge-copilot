package io.github.hyun.sis_app_intelligence.metadata;

public record ProcedureSource(
        String schemaName,
        String procedureName,
        String description,
        String sourceRef,
        String definitionStatus,
        String definition
) {
}
