package io.github.hyun.sis_app_intelligence.metadata;

public record ColumnMeta(
        String columnName,
        Integer ordinalPosition,
        String dataType,
        Boolean nullable,
        String description
) {
}
