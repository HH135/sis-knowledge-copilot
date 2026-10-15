package io.github.hyun.sis_app_intelligence.metadata;

import java.util.List;

public record MetadataSnapshot(
        String databaseName,
        List<TableMeta> tables,
        List<ProcedureMeta> procedures
) {
}
