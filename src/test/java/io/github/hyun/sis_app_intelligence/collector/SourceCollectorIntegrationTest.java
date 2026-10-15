package io.github.hyun.sis_app_intelligence.collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@SpringBootTest
@ActiveProfiles("portfolio")
class SourceCollectorIntegrationTest {

    @Autowired
    private SourceCollectorService sourceCollectorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearPortfolioSourceFiles() {
        jdbcTemplate.update("DELETE FROM source_file WHERE origin = ?", "portfolio");
        jdbcTemplate.update("""
                DELETE FROM source_file
                WHERE origin = 'internal'
                  AND relative_path = 'portfolio-data/app/java/com/acme/svc/appointment/AppointmentController.java'
                """);
    }

    @Test
    void portfolioScanStores54FixtureFilesAndSecondScanIsUnchanged() {
        ScanResult first = sourceCollectorService.scan();
        ScanResult second = sourceCollectorService.scan();

        assertEquals(54, first.scanned());
        assertEquals(54, first.inserted());
        assertEquals(0, first.skipped());
        assertEquals(20, first.byExtension().get("java"));
        assertEquals(8, first.byExtension().get("jsp"));
        assertEquals(6, first.byExtension().get("xml"));
        assertEquals(2, first.byExtension().get("properties") + first.byExtension().get("ini"));
        assertEquals(18, first.byExtension().get("sql"));
        assertEquals(54, second.unchanged());
        assertEquals(0, second.inserted());
        assertEquals(0, second.updated());
    }

    @Test
    void portfolioEndpointReturnsScanResultJson() throws Exception {
        MockMvc mockMvc = standaloneSetup(new SourceScanController(sourceCollectorService)).build();
        mockMvc.perform(post("/api/source-files/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("portfolio"))
                .andExpect(jsonPath("$.scanned").value(54))
                .andExpect(jsonPath("$.byExtension.java").value(20))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    void portfolioScanDoesNotRemoveSamePathFromAnotherOrigin() {
        String path = "portfolio-data/app/java/com/acme/svc/appointment/AppointmentController.java";
        jdbcTemplate.update("""
                INSERT INTO source_file
                    (origin, relative_path, file_type, charset_name, content, sha256, size_bytes)
                VALUES ('internal', ?, 'java', 'UTF-8', 'preserve', ?, 7)
                """, path, "a".repeat(64));

        sourceCollectorService.scan();

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM source_file WHERE origin = 'internal' AND relative_path = ? AND content = 'preserve'",
                Integer.class, path));
    }

    @Test
    void schemaUsesOriginScopedUniquenessAndKeepsSearchVectorBounded() {
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_constraint
                WHERE conname = 'source_file_relative_path_key'
                """, Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_indexes
                WHERE tablename = 'source_file'
                  AND indexname = 'ux_source_file_origin_relative_path'
                  AND indexdef LIKE '%UNIQUE%'
                """, Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_name = 'source_file'
                  AND column_name IN ('origin', 'size_bytes')
                """, Integer.class));
        String searchVectorDefinition = jdbcTemplate.queryForObject("""
                SELECT pg_get_expr(default_value.adbin, default_value.adrelid)
                FROM pg_attrdef default_value
                JOIN pg_class table_info ON table_info.oid = default_value.adrelid
                JOIN pg_attribute column_info
                  ON column_info.attrelid = table_info.oid
                 AND column_info.attnum = default_value.adnum
                WHERE table_info.relname = 'source_file'
                  AND column_info.attname = 'search_vector'
                """, String.class);
        org.junit.jupiter.api.Assertions.assertTrue(searchVectorDefinition.contains("100000"));
    }
}
