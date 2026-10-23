package io.github.hyun.sis_app_intelligence.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("portfolio")
public class DummyMetadataSource implements MetadataSourcePort {

    private static final String TABLE_FIXTURE = "classpath:portfolio-data/metadata/tables.json";
    private static final String PROCEDURE_PATTERN = "classpath:portfolio-data/procedures/*.sql";
    private static final Pattern PROCEDURE_HEADER = Pattern.compile(
            "(?m)^-- Procedure: ([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)\\s*\\R"
                    + "-- Description: (.+)$");

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceResolver;

    public DummyMetadataSource(ObjectMapper objectMapper, ResourcePatternResolver resourceResolver) {
        this.objectMapper = objectMapper;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public MetadataSnapshot load() {
        try {
            JsonNode root = readTableFixture();
            List<TableMeta> tables = parseTables(root.path("tables"));
            List<ProcedureMeta> procedures = parseProcedures();
            return new MetadataSnapshot(root.path("database").asString(), tables, procedures);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not load portfolio metadata fixtures.", exception);
        }
    }

    private JsonNode readTableFixture() throws IOException {
        Resource resource = resourceResolver.getResource(TABLE_FIXTURE);
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        }
    }

    private List<TableMeta> parseTables(JsonNode tableNodes) {
        List<TableMeta> tables = new ArrayList<>();
        for (JsonNode table : tableNodes) {
            List<ColumnMeta> columns = new ArrayList<>();
            for (JsonNode column : table.path("columns")) {
                columns.add(new ColumnMeta(
                        column.path("name").asString(),
                        column.path("ordinal").asInt(),
                        column.path("dataType").asString(),
                        column.path("nullable").asBoolean(),
                        column.path("description").asString()));
            }
            tables.add(new TableMeta(
                    table.path("schema").asString(),
                    table.path("name").asString(),
                    table.path("description").asString(),
                    List.copyOf(columns)));
        }
        return List.copyOf(tables);
    }

    private List<ProcedureMeta> parseProcedures() throws IOException {
        Resource[] resources = resourceResolver.getResources(PROCEDURE_PATTERN);
        List<Resource> sqlResources = new ArrayList<>(List.of(resources));
        sqlResources.sort(Comparator.comparing(Resource::getFilename));

        List<ProcedureMeta> procedures = new ArrayList<>();
        for (Resource resource : sqlResources) {
            String definition;
            try (InputStream input = resource.getInputStream()) {
                definition = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
            }

            Matcher header = PROCEDURE_HEADER.matcher(definition);
            if (!header.find()) {
                throw new IllegalStateException("Procedure fixture has an invalid header: "
                        + resource.getFilename());
            }

            String filename = resource.getFilename();
            String procedureFile = filename.substring("dbo.".length(), filename.length() - ".sql".length());
            if (!procedureFile.equals(header.group(2)) || !"dbo".equals(header.group(1))) {
                throw new IllegalStateException("Procedure header does not match fixture filename: " + filename);
            }

            procedures.add(new ProcedureMeta(
                    header.group(1),
                    header.group(2),
                    header.group(3).trim(),
                    definition,
                    "portfolio-data/procedures/" + filename,
                    "OK"));
        }
        return List.copyOf(procedures);
    }
}
