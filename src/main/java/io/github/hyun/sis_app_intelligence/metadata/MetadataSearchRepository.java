package io.github.hyun.sis_app_intelligence.metadata;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MetadataSearchRepository {

    private static final String COLUMN_SEARCH_SQL = """
            WITH search AS (
                SELECT ?::text AS exact_pattern,
                       ?::text AS prefix_pattern,
                       ?::text AS contains_pattern,
                       ?::text AS table_name
            ),
            matches AS (
                SELECT t.database_name,
                       t.schema_name,
                       t.table_name,
                       t.description AS table_description,
                       c.column_name,
                       c.ordinal_position,
                       c.data_type,
                       c.is_nullable,
                       c.description,
                       CASE
                           WHEN c.column_name ILIKE search.exact_pattern ESCAPE '\\'
                             OR t.table_name ILIKE search.exact_pattern ESCAPE '\\'
                             OR COALESCE(c.description, '') ILIKE search.exact_pattern ESCAPE '\\'
                             OR COALESCE(t.description, '') ILIKE search.exact_pattern ESCAPE '\\' THEN 0
                           WHEN c.column_name ILIKE search.prefix_pattern ESCAPE '\\'
                             OR t.table_name ILIKE search.prefix_pattern ESCAPE '\\'
                             OR COALESCE(c.description, '') ILIKE search.prefix_pattern ESCAPE '\\'
                             OR COALESCE(t.description, '') ILIKE search.prefix_pattern ESCAPE '\\' THEN 1
                           ELSE 2
                       END AS match_rank
                FROM meta_column c
                JOIN meta_table t ON t.id = c.table_id
                CROSS JOIN search
                WHERE (
                       c.column_name ILIKE search.contains_pattern ESCAPE '\\'
                    OR t.table_name ILIKE search.contains_pattern ESCAPE '\\'
                    OR COALESCE(c.description, '') ILIKE search.contains_pattern ESCAPE '\\'
                    OR COALESCE(t.description, '') ILIKE search.contains_pattern ESCAPE '\\'
                )
                  AND (search.table_name IS NULL OR lower(t.table_name) = lower(search.table_name))
            )
            SELECT database_name, schema_name, table_name, table_description, column_name,
                   ordinal_position, data_type, is_nullable, description
            FROM matches
            ORDER BY match_rank, column_name, schema_name, table_name
            LIMIT ?
            """;

    private static final String PROCEDURE_SEARCH_SQL = """
            WITH search AS (
                SELECT ?::text AS exact_pattern,
                       ?::text AS prefix_pattern,
                       ?::text AS contains_pattern,
                       ?::boolean AS search_body
            ),
            matches AS (
                SELECT p.database_name,
                       p.schema_name,
                       p.procedure_name,
                       p.description,
                       p.source_ref,
                       p.definition_status,
                       CASE
                           WHEN p.procedure_name ILIKE search.exact_pattern ESCAPE '\\' THEN 'name'
                           WHEN COALESCE(p.description, '') ILIKE search.exact_pattern ESCAPE '\\' THEN 'description'
                           WHEN search.search_body
                            AND COALESCE(p.definition, '') ILIKE search.exact_pattern ESCAPE '\\' THEN 'body'
                           WHEN p.procedure_name ILIKE search.prefix_pattern ESCAPE '\\' THEN 'name'
                           WHEN COALESCE(p.description, '') ILIKE search.prefix_pattern ESCAPE '\\' THEN 'description'
                           WHEN search.search_body
                            AND COALESCE(p.definition, '') ILIKE search.prefix_pattern ESCAPE '\\' THEN 'body'
                           WHEN p.procedure_name ILIKE search.contains_pattern ESCAPE '\\' THEN 'name'
                           WHEN COALESCE(p.description, '') ILIKE search.contains_pattern ESCAPE '\\' THEN 'description'
                           ELSE 'body'
                       END AS matched_in,
                       CASE
                           WHEN p.procedure_name ILIKE search.exact_pattern ESCAPE '\\'
                             OR COALESCE(p.description, '') ILIKE search.exact_pattern ESCAPE '\\'
                             OR (search.search_body AND COALESCE(p.definition, '') ILIKE search.exact_pattern ESCAPE '\\') THEN 0
                           WHEN p.procedure_name ILIKE search.prefix_pattern ESCAPE '\\'
                             OR COALESCE(p.description, '') ILIKE search.prefix_pattern ESCAPE '\\'
                             OR (search.search_body AND COALESCE(p.definition, '') ILIKE search.prefix_pattern ESCAPE '\\') THEN 1
                           ELSE 2
                       END AS match_rank
                FROM meta_proc p
                CROSS JOIN search
                WHERE p.procedure_name ILIKE search.contains_pattern ESCAPE '\\'
                   OR COALESCE(p.description, '') ILIKE search.contains_pattern ESCAPE '\\'
                   OR (search.search_body AND COALESCE(p.definition, '') ILIKE search.contains_pattern ESCAPE '\\')
            )
            SELECT database_name, schema_name, procedure_name, description, source_ref,
                   definition_status, matched_in
            FROM matches
            ORDER BY match_rank, procedure_name
            LIMIT ?
            """;

    private static final RowMapper<ColumnSearchItem> COLUMN_ROW_MAPPER = (resultSet, rowNum) ->
            new ColumnSearchItem(
                    resultSet.getString("database_name"),
                    resultSet.getString("schema_name"),
                    resultSet.getString("table_name"),
                    resultSet.getString("table_description"),
                    resultSet.getString("column_name"),
                    (Integer) resultSet.getObject("ordinal_position"),
                    resultSet.getString("data_type"),
                    (Boolean) resultSet.getObject("is_nullable"),
                    resultSet.getString("description"));

    private static final RowMapper<ProcedureSearchItem> PROCEDURE_ROW_MAPPER = (resultSet, rowNum) ->
            new ProcedureSearchItem(
                    resultSet.getString("database_name"),
                    resultSet.getString("schema_name"),
                    resultSet.getString("procedure_name"),
                    resultSet.getString("description"),
                    resultSet.getString("source_ref"),
                    resultSet.getString("definition_status"),
                    resultSet.getString("matched_in"));

    private final JdbcTemplate jdbcTemplate;

    public MetadataSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ColumnSearchItem> searchColumns(String exactPattern, String prefixPattern,
                                                String containsPattern, String tableName, int limit) {
        return jdbcTemplate.query(COLUMN_SEARCH_SQL, COLUMN_ROW_MAPPER,
                exactPattern, prefixPattern, containsPattern, tableName, limit);
    }

    public List<ProcedureSearchItem> searchProcedures(String exactPattern, String prefixPattern,
                                                      String containsPattern, boolean searchBody, int limit) {
        return jdbcTemplate.query(PROCEDURE_SEARCH_SQL, PROCEDURE_ROW_MAPPER,
                exactPattern, prefixPattern, containsPattern, searchBody, limit);
    }
}
