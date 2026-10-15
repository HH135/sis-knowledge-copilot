package io.github.hyun.sis_app_intelligence.metadata;

import java.util.List;

public record TableMeta(
        String schemaName,
        String tableName,
        String description,
        List<ColumnMeta> columns
) {
}
