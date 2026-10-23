package io.github.hyun.sis_app_intelligence.document;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.github.hyun.sis_app_intelligence.metadata.ProcedureMeta;
import io.github.hyun.sis_app_intelligence.source.SourceFile;

@Repository
public class DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SourceFile> findSourceFiles(String origin) {
        return jdbcTemplate.query("""
                SELECT origin, relative_path, file_type, charset_name, content, sha256, size_bytes
                FROM source_file
                WHERE origin = ?
                ORDER BY relative_path
                """, (resultSet, rowNum) -> new SourceFile(
                resultSet.getString("origin"),
                resultSet.getString("relative_path"),
                resultSet.getString("file_type"),
                resultSet.getString("charset_name"),
                resultSet.getString("content"),
                resultSet.getString("sha256"),
                resultSet.getLong("size_bytes")), origin);
    }

    public List<TableChunkData> findTables() {
        return jdbcTemplate.query("""
                SELECT t.schema_name, t.table_name, t.description,
                       COALESCE(string_agg(
                           concat(c.column_name, ' ', COALESCE(c.data_type, ''),
                                  ' ', CASE WHEN c.is_nullable THEN 'NULL' ELSE 'NOT NULL' END,
                                  ' ', COALESCE(c.description, '')),
                           E'\\n' ORDER BY c.ordinal_position), '') AS columns_text
                FROM meta_table t
                LEFT JOIN meta_column c ON c.table_id = t.id
                GROUP BY t.id, t.schema_name, t.table_name, t.description
                ORDER BY t.schema_name, t.table_name
                """, (resultSet, rowNum) -> new TableChunkData(
                resultSet.getString("schema_name"),
                resultSet.getString("table_name"),
                resultSet.getString("description"),
                resultSet.getString("columns_text")));
    }

    public List<ProcedureMeta> findProcedures() {
        return jdbcTemplate.query("""
                SELECT schema_name, procedure_name, description, definition, source_ref, definition_status
                FROM meta_proc
                ORDER BY schema_name, procedure_name
                """, (resultSet, rowNum) -> new ProcedureMeta(
                resultSet.getString("schema_name"),
                resultSet.getString("procedure_name"),
                resultSet.getString("description"),
                resultSet.getString("definition"),
                resultSet.getString("source_ref"),
                resultSet.getString("definition_status")));
    }

    public void deleteByOrigin(String origin) {
        jdbcTemplate.update("DELETE FROM document_chunk WHERE origin = ?", origin);
    }

    public void insert(ChunkDraft chunk) {
        jdbcTemplate.update("""
                INSERT INTO document_chunk
                    (origin, source_kind, source_ref, title, start_line, end_line, content, content_sha256)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                chunk.origin(),
                chunk.sourceKind(),
                chunk.sourceRef(),
                chunk.title(),
                chunk.startLine(),
                chunk.endLine(),
                chunk.content(),
                chunk.contentSha256());
    }

    public record TableChunkData(String schemaName, String tableName, String description, String columnsText) {
    }
}
