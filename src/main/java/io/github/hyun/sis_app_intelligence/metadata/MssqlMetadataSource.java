package io.github.hyun.sis_app_intelligence.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.sql.Driver;
import java.util.Properties;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.hyun.sis_app_intelligence.config.MssqlProperties;

/**
 * Reads SQL Server catalog metadata using an account limited to db_datareader and VIEW DEFINITION.
 * Read-only access is enforced by those database permissions, not by Connection.setReadOnly.
 */
@Component
@Profile("internal")
public class MssqlMetadataSource implements MetadataSourcePort {

    private static final Logger LOGGER = Logger.getLogger(MssqlMetadataSource.class.getName());

    private static final String DATABASE_NAME_SQL = "SELECT DB_NAME()";

    private static final String TABLES_SQL = """
            SELECT s.name AS schema_name,
                   t.name AS table_name,
                   CAST(ep.value AS nvarchar(max)) AS description
            FROM sys.tables t
            JOIN sys.schemas s ON s.schema_id = t.schema_id
            LEFT JOIN sys.extended_properties ep
              ON ep.class = 1
             AND ep.major_id = t.object_id
             AND ep.minor_id = 0
             AND ep.name = 'MS_Description'
            WHERE t.is_ms_shipped = 0
            ORDER BY s.name, t.name
            """;

    private static final String COLUMNS_SQL = """
            SELECT s.name AS schema_name,
                   t.name AS table_name,
                   c.name AS column_name,
                   c.column_id AS ordinal_position,
                   ty.name AS type_name,
                   c.max_length,
                   c.precision,
                   c.scale,
                   c.is_nullable,
                   CAST(ep.value AS nvarchar(max)) AS description
            FROM sys.columns c
            JOIN sys.tables t ON t.object_id = c.object_id
            JOIN sys.schemas s ON s.schema_id = t.schema_id
            JOIN sys.types ty ON ty.user_type_id = c.user_type_id
            LEFT JOIN sys.extended_properties ep
              ON ep.class = 1
             AND ep.major_id = c.object_id
             AND ep.minor_id = c.column_id
             AND ep.name = 'MS_Description'
            WHERE t.is_ms_shipped = 0
            ORDER BY s.name, t.name, c.column_id
            """;

    private static final String PROCEDURES_SQL = """
            SELECT s.name AS schema_name,
                   p.name AS procedure_name,
                   m.definition,
                   CAST(ep.value AS nvarchar(max)) AS description
            FROM sys.procedures p
            JOIN sys.schemas s ON s.schema_id = p.schema_id
            LEFT JOIN sys.sql_modules m ON m.object_id = p.object_id
            LEFT JOIN sys.extended_properties ep
              ON ep.class = 1
             AND ep.major_id = p.object_id
             AND ep.minor_id = 0
             AND ep.name = 'MS_Description'
            WHERE p.is_ms_shipped = 0
            ORDER BY s.name, p.name
            """;

    private static final RowMapper<TableRow> TABLE_ROW_MAPPER = (resultSet, rowNum) ->
            new TableRow(
                    resultSet.getString("schema_name"),
                    resultSet.getString("table_name"),
                    resultSet.getString("description"));

    private static final RowMapper<ColumnRow> COLUMN_ROW_MAPPER = (resultSet, rowNum) ->
            new ColumnRow(
                    resultSet.getString("schema_name"),
                    resultSet.getString("table_name"),
                    resultSet.getString("column_name"),
                    resultSet.getInt("ordinal_position"),
                    resultSet.getString("type_name"),
                    resultSet.getInt("max_length"),
                    resultSet.getInt("precision"),
                    resultSet.getInt("scale"),
                    resultSet.getBoolean("is_nullable"),
                    resultSet.getString("description"));

    private static final RowMapper<ProcedureRow> PROCEDURE_ROW_MAPPER = (resultSet, rowNum) ->
            new ProcedureRow(
                    resultSet.getString("schema_name"),
                    resultSet.getString("procedure_name"),
                    resultSet.getString("definition"),
                    resultSet.getString("description"));

    private final MssqlProperties properties;
    private final JdbcTemplate jdbcTemplateOverride;

    @Autowired
    public MssqlMetadataSource(MssqlProperties properties) {
        this(properties, null);
    }

    MssqlMetadataSource(MssqlProperties properties, JdbcTemplate jdbcTemplateOverride) {
        this.properties = properties;
        this.jdbcTemplateOverride = jdbcTemplateOverride;
    }

    @Override
    public MetadataSnapshot load() {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new MssqlNotConfiguredException();
        }

        try {
            JdbcTemplate jdbcTemplate = createJdbcTemplate();
            String databaseName = jdbcTemplate.queryForObject(DATABASE_NAME_SQL, String.class);
            List<TableRow> tables = jdbcTemplate.query(TABLES_SQL, TABLE_ROW_MAPPER);
            List<ColumnRow> columns = jdbcTemplate.query(COLUMNS_SQL, COLUMN_ROW_MAPPER);
            List<ProcedureRow> procedures = jdbcTemplate.query(PROCEDURES_SQL, PROCEDURE_ROW_MAPPER);

            MetadataSnapshot snapshot = createSnapshot(databaseName, tables, columns, procedures);
            LOGGER.info(() -> "Loaded SQL Server metadata: tables=" + tables.size()
                    + ", columns=" + columns.size() + ", procedures=" + procedures.size());
            return snapshot;
        }
        catch (DataAccessException exception) {
            throw new MssqlUnavailableException();
        }
    }

    private JdbcTemplate createJdbcTemplate() {
        if (jdbcTemplateOverride != null) {
            return jdbcTemplateOverride;
        }

        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        try {
            Class<? extends Driver> driverClass = Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                    .asSubclass(Driver.class);
            dataSource.setDriverClass(driverClass);
        }
        catch (ClassNotFoundException exception) {
            throw new MssqlUnavailableException();
        }
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("loginTimeout",
                Integer.toString(properties.getLoginTimeoutSeconds()));
        dataSource.setConnectionProperties(connectionProperties);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(properties.getQueryTimeoutSeconds());
        return jdbcTemplate;
    }

    private MetadataSnapshot createSnapshot(
            String databaseName,
            List<TableRow> tableRows,
            List<ColumnRow> columnRows,
            List<ProcedureRow> procedureRows) {
        Map<TableKey, List<ColumnMeta>> columnsByTable = new LinkedHashMap<>();
        for (ColumnRow column : columnRows) {
            TableKey key = new TableKey(column.schemaName(), column.tableName());
            columnsByTable.computeIfAbsent(key, unused -> new ArrayList<>()).add(new ColumnMeta(
                    column.columnName(),
                    column.ordinalPosition(),
                    normalizeDataType(column.typeName(), column.maxLength(), column.precision(), column.scale()),
                    column.nullable(),
                    column.description()));
        }

        List<TableMeta> tables = tableRows.stream()
                .map(table -> {
                    TableKey key = new TableKey(table.schemaName(), table.tableName());
                    return new TableMeta(table.schemaName(), table.tableName(), table.description(),
                            List.copyOf(columnsByTable.getOrDefault(key, List.of())));
                })
                .toList();

        List<ProcedureMeta> procedures = procedureRows.stream()
                .map(procedure -> new ProcedureMeta(
                        procedure.schemaName(),
                        procedure.procedureName(),
                        procedure.description(),
                        procedure.definition(),
                        "mssql:" + databaseName + "." + procedure.schemaName() + "." + procedure.procedureName(),
                        procedure.definition() == null ? "NO_ACCESS" : "OK"))
                .toList();

        return new MetadataSnapshot(databaseName, tables, procedures);
    }

    static String normalizeDataType(String typeName, int maxLength, int precision, int scale) {
        if ("decimal".equalsIgnoreCase(typeName) || "numeric".equalsIgnoreCase(typeName)) {
            return typeName + "(" + precision + "," + scale + ")";
        }
        if ("varchar".equalsIgnoreCase(typeName) || "char".equalsIgnoreCase(typeName)
                || "varbinary".equalsIgnoreCase(typeName) || "binary".equalsIgnoreCase(typeName)) {
            return typeName + (maxLength == -1 ? "(max)" : "(" + maxLength + ")");
        }
        if ("nvarchar".equalsIgnoreCase(typeName) || "nchar".equalsIgnoreCase(typeName)) {
            return typeName + (maxLength == -1 ? "(max)" : "(" + maxLength / 2 + ")");
        }
        return typeName;
    }

    static List<String> catalogQueries() {
        return List.of(DATABASE_NAME_SQL, TABLES_SQL, COLUMNS_SQL, PROCEDURES_SQL);
    }

    private record TableKey(String schemaName, String tableName) {
    }

    private record TableRow(String schemaName, String tableName, String description) {
    }

    private record ColumnRow(
            String schemaName,
            String tableName,
            String columnName,
            int ordinalPosition,
            String typeName,
            int maxLength,
            int precision,
            int scale,
            boolean nullable,
            String description) {
    }

    private record ProcedureRow(
            String schemaName,
            String procedureName,
            String definition,
            String description) {
    }
}
