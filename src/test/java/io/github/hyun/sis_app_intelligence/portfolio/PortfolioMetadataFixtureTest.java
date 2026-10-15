package io.github.hyun.sis_app_intelligence.portfolio;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioMetadataFixtureTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void operationalMetadataDefinesTwentyTablesWithFiveToFifteenColumns() throws Exception {
		JsonNode metadata = readJson("portfolio-data/metadata/tables.json");

		assertEquals("ACME_SVC", metadata.path("database").asString());
		JsonNode tables = metadata.path("tables");
		assertEquals(20, tables.size());

		Set<String> tableNames = new HashSet<>();
		for (JsonNode table : tables) {
			assertTrue(tableNames.add(table.path("schema").asString() + "." + table.path("name").asString()));
			assertTrue(table.path("columns").size() >= 5);
			assertTrue(table.path("columns").size() <= 15);
		}
	}

	@Test
	void specificationContainsExactlyTheFourDocumentedDifferences() throws Exception {
		JsonNode metadata = readJson("portfolio-data/metadata/tables.json");
		JsonNode specification = readJson("portfolio-data/spec/table-spec.json");
		Map<String, JsonNode> metadataTables = tablesByName(metadata);
		Map<String, JsonNode> specificationTables = tablesByName(specification);

		assertEquals(metadata.path("database").asString(), specification.path("database").asString());
		assertEquals(metadataTables.keySet(), specificationTables.keySet());

		Set<String> metadataOnlyColumns = new HashSet<>();
		Set<String> specificationOnlyColumns = new HashSet<>();
		Set<String> typeMismatches = new HashSet<>();
		Set<String> descriptionMismatches = new HashSet<>();

		for (String tableKey : metadataTables.keySet()) {
			Map<String, JsonNode> metadataColumns = columnsByName(metadataTables.get(tableKey));
			Map<String, JsonNode> specificationColumns = columnsByName(specificationTables.get(tableKey));

			for (String columnName : metadataColumns.keySet()) {
				String location = tableKey + "." + columnName;
				if (!specificationColumns.containsKey(columnName)) {
					metadataOnlyColumns.add(location);
					continue;
				}

				JsonNode metadataColumn = metadataColumns.get(columnName);
				JsonNode specificationColumn = specificationColumns.get(columnName);
				if (!metadataColumn.path("dataType").asString()
						.equals(specificationColumn.path("dataType").asString())) {
					typeMismatches.add(location);
				}
				if (!metadataColumn.path("description").asString()
						.equals(specificationColumn.path("description").asString())) {
					descriptionMismatches.add(location);
				}
			}

			for (String columnName : specificationColumns.keySet()) {
				if (!metadataColumns.containsKey(columnName)) {
					specificationOnlyColumns.add(tableKey + "." + columnName);
				}
			}
		}

		assertEquals(Set.of("dbo.TB_VEHICLE.COLOR_CODE"), metadataOnlyColumns);
		assertEquals(Set.of("dbo.TB_CUSTOMER.EMAIL_OPT_IN"), specificationOnlyColumns);
		assertEquals(Set.of("dbo.TB_CUSTOMER.PHONE_NO"), typeMismatches);
		assertEquals(Set.of("dbo.TB_REPAIR_ORDER.ORDER_STATUS"), descriptionMismatches);
		assertEquals(4, metadataOnlyColumns.size() + specificationOnlyColumns.size()
				+ typeMismatches.size() + descriptionMismatches.size());
		assertFalse(metadataTables.isEmpty());
	}

	private JsonNode readJson(String resourcePath) throws Exception {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			assertTrue(input != null, "Missing classpath resource: " + resourcePath);
			return objectMapper.readTree(input);
		}
	}

	private Map<String, JsonNode> tablesByName(JsonNode metadata) {
		Map<String, JsonNode> tables = new HashMap<>();
		for (JsonNode table : metadata.path("tables")) {
			String key = table.path("schema").asString() + "." + table.path("name").asString();
			tables.put(key, table);
		}
		return tables;
	}

	private Map<String, JsonNode> columnsByName(JsonNode table) {
		Map<String, JsonNode> columns = new HashMap<>();
		for (JsonNode column : table.path("columns")) {
			columns.put(column.path("name").asString(), column);
		}
		return columns;
	}
}
