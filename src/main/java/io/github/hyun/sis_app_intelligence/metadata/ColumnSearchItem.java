package io.github.hyun.sis_app_intelligence.metadata;

public record ColumnSearchItem(
        String databaseName,
        String schemaName,
        String tableName,
        String tableDescription,
        String columnName,
        Integer ordinal,
        String dataType,
        Boolean nullable,
        String description
) {
}
