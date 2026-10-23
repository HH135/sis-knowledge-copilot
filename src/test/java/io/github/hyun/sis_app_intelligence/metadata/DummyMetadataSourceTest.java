package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DummyMetadataSourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsOnlyOperationalTablesAndProcedureFixtures() throws Exception {
        DummyMetadataSource source = new DummyMetadataSource(
                objectMapper, new PathMatchingResourcePatternResolver());
        MetadataSnapshot snapshot = source.load();

        assertEquals("ACME_SVC", snapshot.databaseName());
        assertEquals(20, snapshot.tables().size());
        assertEquals(expectedColumnCount(), snapshot.tables().stream()
                .mapToInt(table -> table.columns().size())
                .sum());
        assertEquals(18, snapshot.procedures().size());
        assertTrue(snapshot.procedures().stream()
                .allMatch(procedure -> procedure.description() != null && !procedure.description().isBlank()));
        assertTrue(snapshot.procedures().stream()
                .allMatch(procedure -> procedure.definition() != null && !procedure.definition().isBlank()));
        assertTrue(snapshot.procedures().stream()
                .allMatch(procedure -> procedure.sourceRef().startsWith("portfolio-data/procedures/dbo.USP_")
                        && procedure.sourceRef().endsWith(".sql")));
        assertTrue(snapshot.procedures().stream()
                .allMatch(procedure -> "OK".equals(procedure.definitionStatus())));

        Set<String> columnNames = snapshot.tables().stream()
                .flatMap(table -> table.columns().stream())
                .map(ColumnMeta::columnName)
                .collect(Collectors.toSet());
        assertFalse(columnNames.contains("EMAIL_OPT_IN"));
    }

    private int expectedColumnCount() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("portfolio-data/metadata/tables.json")) {
            assertNotNull(input);
            JsonNode tables = objectMapper.readTree(input).path("tables");
            int count = 0;
            for (JsonNode table : tables) {
                count += table.path("columns").size();
            }
            return count;
        }
    }
}
