package io.github.hyun.sis_app_intelligence.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import io.github.hyun.sis_app_intelligence.config.MssqlProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MssqlMetadataSourceTest {

    private static final Pattern FORBIDDEN_SQL = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|MERGE|EXEC|EXECUTE|DROP|ALTER|CREATE|TRUNCATE|GRANT)\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    void allCatalogSqlConstantsAreSelectOnly() throws Exception {
        List<String> queryConstants = java.util.Arrays.stream(MssqlMetadataSource.class.getDeclaredFields())
                .filter(field -> Modifier.isPrivate(field.getModifiers()))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> Modifier.isFinal(field.getModifiers()))
                .filter(field -> field.getName().endsWith("_SQL"))
                .map(this::stringValue)
                .toList();

        assertEquals(4, queryConstants.size());
        for (String query : queryConstants) {
            assertTrue(query.stripLeading().startsWith("SELECT"));
            assertFalse(FORBIDDEN_SQL.matcher(query).find(), query);
        }
    }

    @Test
    void normalizesCommonSqlServerTypes() {
        assertEquals("varchar(20)", MssqlMetadataSource.normalizeDataType("varchar", 20, 0, 0));
        assertEquals("nvarchar(50)", MssqlMetadataSource.normalizeDataType("nvarchar", 100, 0, 0));
        assertEquals("varchar(max)", MssqlMetadataSource.normalizeDataType("varchar", -1, 0, 0));
        assertEquals("decimal(18,2)", MssqlMetadataSource.normalizeDataType("decimal", 9, 18, 2));
        assertEquals("int", MssqlMetadataSource.normalizeDataType("int", 4, 10, 0));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapsNullDefinitionToNoAccessAndMissingDescriptionsToNull() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MssqlProperties properties = configuredProperties();
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class))).thenReturn("ExampleDb");
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            RowMapper<Object> rowMapper = invocation.getArgument(1);
            ResultSet resultSet = resultSetFor(sql);
            return List.of(rowMapper.mapRow(resultSet, 0));
        });
        MssqlMetadataSource source = new MssqlMetadataSource(properties, jdbcTemplate);

        MetadataSnapshot snapshot = source.load();

        assertEquals("ExampleDb", snapshot.databaseName());
        assertEquals(1, snapshot.tables().size());
        assertNull(snapshot.tables().getFirst().description());
        assertNull(snapshot.tables().getFirst().columns().getFirst().description());
        assertEquals("int", snapshot.tables().getFirst().columns().getFirst().dataType());
        ProcedureMeta procedure = snapshot.procedures().getFirst();
        assertNull(procedure.definition());
        assertNull(procedure.description());
        assertEquals("NO_ACCESS", procedure.definitionStatus());
        assertEquals("mssql:ExampleDb.dbo.USP_SAMPLE", procedure.sourceRef());
    }

    @Test
    void convertsJdbcConnectionFailuresToSanitizedUnavailableError() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class)))
                .thenThrow(new DataAccessResourceFailureException("sensitive connection details"));
        MssqlMetadataSource source = new MssqlMetadataSource(configuredProperties(), jdbcTemplate);

        org.junit.jupiter.api.Assertions.assertThrows(MssqlUnavailableException.class, source::load);
    }

    private ResultSet resultSetFor(String sql) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        List<String> queries = MssqlMetadataSource.catalogQueries();
        if (sql.equals(queries.get(1))) {
            when(resultSet.getString("schema_name")).thenReturn("dbo");
            when(resultSet.getString("table_name")).thenReturn("T_SAMPLE");
        }
        else if (sql.equals(queries.get(2))) {
            when(resultSet.getString("schema_name")).thenReturn("dbo");
            when(resultSet.getString("table_name")).thenReturn("T_SAMPLE");
            when(resultSet.getString("column_name")).thenReturn("ID");
            when(resultSet.getInt("ordinal_position")).thenReturn(1);
            when(resultSet.getString("type_name")).thenReturn("int");
            when(resultSet.getInt("max_length")).thenReturn(4);
            when(resultSet.getInt("precision")).thenReturn(10);
            when(resultSet.getInt("scale")).thenReturn(0);
            when(resultSet.getBoolean("is_nullable")).thenReturn(false);
        }
        else {
            when(resultSet.getString("schema_name")).thenReturn("dbo");
            when(resultSet.getString("procedure_name")).thenReturn("USP_SAMPLE");
            when(resultSet.getString("definition")).thenReturn(null);
        }
        when(resultSet.getString("description")).thenReturn(null);
        return resultSet;
    }

    private String stringValue(Field field) {
        try {
            field.setAccessible(true);
            return (String) field.get(null);
        }
        catch (IllegalAccessException exception) {
            throw new AssertionError(exception);
        }
    }

    private MssqlProperties configuredProperties() {
        MssqlProperties properties = new MssqlProperties();
        properties.setUrl("configured");
        return properties;
    }
}
