package io.github.hyun.sis_app_intelligence.metadata;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MetadataRepository {

    private final JdbcTemplate jdbcTemplate;

    public MetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replace(MetadataSnapshot snapshot) {
        String databaseName = snapshot.databaseName();
        jdbcTemplate.update("DELETE FROM meta_table WHERE database_name = ?", databaseName);
        jdbcTemplate.update("DELETE FROM meta_proc WHERE database_name = ?", databaseName);

        for (TableMeta table : snapshot.tables()) {
            Long tableId = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO meta_table (database_name, schema_name, table_name, description)
                    VALUES (?, ?, ?, ?)
                    RETURNING id
                    """,
                    Long.class,
                    databaseName,
                    table.schemaName(),
                    table.tableName(),
                    table.description());

            for (ColumnMeta column : table.columns()) {
                jdbcTemplate.update(
                        """
                        INSERT INTO meta_column
                            (table_id, column_name, ordinal_position, data_type, is_nullable, description)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        tableId,
                        column.columnName(),
                        column.ordinalPosition(),
                        column.dataType(),
                        column.nullable(),
                        column.description());
            }
        }

        for (ProcedureMeta procedure : snapshot.procedures()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO meta_proc
                        (database_name, schema_name, procedure_name, definition, description, source_ref, definition_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    databaseName,
                    procedure.schemaName(),
                    procedure.procedureName(),
                    procedure.definition(),
                    procedure.description(),
                        procedure.sourceRef(),
                        procedure.definitionStatus());
        }
    }
}
