package io.github.hyun.sis_app_intelligence.dependency;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.github.hyun.sis_app_intelligence.metadata.ProcedureMeta;
import io.github.hyun.sis_app_intelligence.source.SourceFile;

@Repository
public class DependencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public DependencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    public List<SourceFile> findMapperFiles(String origin) {
        return jdbcTemplate.query("""
                SELECT origin, relative_path, file_type, charset_name, content, sha256, size_bytes
                FROM source_file
                WHERE origin = ?
                  AND lower(file_type) = 'xml'
                  AND lower(replace(relative_path, '\\', '/')) LIKE '%/mapper/%.xml'
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

    public void deleteByOrigin(String origin) {
        jdbcTemplate.update("DELETE FROM proc_dependency WHERE origin = ?", origin);
    }

    public void insert(DependencyEdge edge) {
        jdbcTemplate.update("""
                INSERT INTO proc_dependency
                    (origin, caller_type, caller_ref, callee_schema, callee_name, call_type, line_no)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (origin, caller_type, caller_ref, callee_schema, callee_name, line_no)
                DO UPDATE SET call_type = EXCLUDED.call_type
                """,
                edge.origin(),
                edge.callerType(),
                edge.callerRef(),
                edge.calleeSchema(),
                edge.calleeName(),
                edge.callType(),
                edge.lineNo());
    }

    public boolean procedureExists(String origin, String procedure) {
        ProcedureKey key = parseProcedure(procedure);
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM meta_proc
                    WHERE lower(schema_name) = lower(?) AND lower(procedure_name) = lower(?)
                )
                """, Boolean.class, key.schema(), key.name()));
    }

    public List<DependencyEdge> findOutgoing(String origin, String procedureRef) {
        return jdbcTemplate.query("""
                SELECT origin, caller_type, caller_ref, callee_schema, callee_name, call_type, line_no
                FROM proc_dependency
                WHERE origin = ? AND caller_type = 'PROCEDURE' AND lower(caller_ref) = lower(?)
                ORDER BY line_no NULLS LAST, callee_schema, callee_name
                """, this::mapEdge, origin, procedureRef);
    }

    public List<CallerItem> findCallers(String origin, String procedure) {
        ProcedureKey key = parseProcedure(procedure);
        return jdbcTemplate.query("""
                SELECT caller_type, caller_ref, call_type, line_no
                FROM proc_dependency
                WHERE origin = ? AND lower(callee_schema) = lower(?) AND lower(callee_name) = lower(?)
                ORDER BY caller_type, caller_ref
                """, (resultSet, rowNum) -> new CallerItem(
                resultSet.getString("caller_type"),
                resultSet.getString("caller_ref"),
                resultSet.getString("call_type"),
                (Integer) resultSet.getObject("line_no")), origin, key.schema(), key.name());
    }

    private DependencyEdge mapEdge(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new DependencyEdge(
                resultSet.getString("origin"),
                resultSet.getString("caller_type"),
                resultSet.getString("caller_ref"),
                resultSet.getString("callee_schema"),
                resultSet.getString("callee_name"),
                resultSet.getString("call_type"),
                (Integer) resultSet.getObject("line_no"));
    }

    private ProcedureKey parseProcedure(String procedure) {
        String normalized = procedure == null ? "" : procedure.trim().replace("[", "").replace("]", "");
        int separator = normalized.lastIndexOf('.');
        if (separator < 0) {
            return new ProcedureKey("dbo", normalized);
        }
        return new ProcedureKey(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    private record ProcedureKey(String schema, String name) {
    }
}
