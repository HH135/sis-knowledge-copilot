package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("portfolio")
class MetadataSyncIntegrationTest {

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void repeatedSyncReplacesMetadataAndCascadesColumns() {
        SyncResult first = metadataSyncService.sync();
        SyncResult second = metadataSyncService.sync();

        assertEquals(first, second);
        assertEquals(first.tableCount(), count(
                "SELECT count(*) FROM meta_table WHERE database_name = ?", first.databaseName()));
        assertEquals(first.columnCount(), count(
                """
                SELECT count(*)
                FROM meta_column c
                JOIN meta_table t ON t.id = c.table_id
                WHERE t.database_name = ?
                """, first.databaseName()));
        assertEquals(first.procedureCount(), count(
                "SELECT count(*) FROM meta_proc WHERE database_name = ?", first.databaseName()));
        assertEquals(first.procedureCount(), jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM meta_proc
                WHERE database_name = ?
                  AND description IS NOT NULL
                  AND source_ref LIKE 'portfolio-data/procedures/%.sql'
                """,
                Integer.class,
                first.databaseName()));
    }

    private int count(String sql, String databaseName) {
        return jdbcTemplate.queryForObject(sql, Integer.class, databaseName);
    }
}
