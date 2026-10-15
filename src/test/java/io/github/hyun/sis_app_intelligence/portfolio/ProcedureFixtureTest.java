package io.github.hyun.sis_app_intelligence.portfolio;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcedureFixtureTest {

	private static final Pattern PROCEDURE_HEADER = Pattern.compile(
			"(?m)^-- Procedure: (dbo\\.USP_[A-Z_]+)\\s*\\R"
					+ "-- Description: (.+)\\R"
					+ "-- Tables: (.+)$");
	private static final Pattern CALL_EDGE = Pattern.compile(
			"(?m)^\\| `?(dbo\\.USP_[A-Z_]+)`? \\| `?(dbo\\.USP_[A-Z_]+)`? \\| Resolved \\|$");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void hasEighteenWellFormedProcedureFilesUsingOnlyKnownTables() throws Exception {
		JsonNode metadata = readJson("portfolio-data/metadata/tables.json");
		Set<String> knownTables = new HashSet<>();
		for (JsonNode table : metadata.path("tables")) {
			knownTables.add(table.path("name").asString());
		}

		Path procedureDirectory = resourcePath("portfolio-data/procedures");
		try (Stream<Path> procedureFiles = Files.list(procedureDirectory)) {
			var files = procedureFiles
					.filter(path -> path.getFileName().toString().endsWith(".sql"))
					.toList();
			assertEquals(18, files.size());

			for (Path file : files) {
				String sql = Files.readString(file, StandardCharsets.UTF_8);
				Matcher header = PROCEDURE_HEADER.matcher(sql);
				assertTrue(header.find(), "Procedure header missing in " + file.getFileName());

				String expectedFileName = header.group(1) + ".sql";
				assertEquals(expectedFileName, file.getFileName().toString());
				assertTrue(!header.group(2).isBlank(), "Description is blank in " + file.getFileName());

				for (String tableName : header.group(3).split("\\s*,\\s*")) {
					assertTrue(knownTables.contains(tableName),
							"Unknown table " + tableName + " in " + file.getFileName());
				}
			}
		}
	}

	@Test
	void everyDocumentedResolvedCalleeHasAProcedureFileAndLongestChainIsPresent() throws Exception {
		String expectedCalls = Files.readString(
				resourcePath("portfolio-data/procedures/EXPECTED_CALLS.md"), StandardCharsets.UTF_8);
		Set<String> procedureNames = procedureNames();
		Matcher edges = CALL_EDGE.matcher(expectedCalls);
		Set<String> resolvedEdges = new HashSet<>();

		while (edges.find()) {
			String caller = edges.group(1);
			String callee = edges.group(2);
			assertTrue(procedureNames.contains(caller), "Missing caller procedure file: " + caller);
			assertTrue(procedureNames.contains(callee), "Missing callee procedure file: " + callee);
			resolvedEdges.add(caller + " -> " + callee);
		}

		assertEquals(8, resolvedEdges.size());
		assertTrue(resolvedEdges.contains(
				"dbo.USP_COMPLETE_REPAIR_ORDER -> dbo.USP_CONFIRM_PAYMENT"));
		assertTrue(resolvedEdges.contains(
				"dbo.USP_CONFIRM_PAYMENT -> dbo.USP_REDEEM_COUPON"));
		assertTrue(resolvedEdges.contains(
				"dbo.USP_REDEEM_COUPON -> dbo.USP_WRITE_INTERFACE_LOG"));
		assertTrue(expectedCalls.contains("dbo.USP_LEGACY_SYNC"));
		assertTrue(expectedCalls.contains("sys.sp_executesql"));
		assertTrue(expectedCalls.contains("comment"));
		assertFalse(procedureNames.contains("dbo.USP_LEGACY_SYNC"));
	}

	private JsonNode readJson(String resourceName) throws Exception {
		try (var input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			assertTrue(input != null, "Missing classpath resource: " + resourceName);
			return objectMapper.readTree(input);
		}
	}

	private Path resourcePath(String resourceName) throws Exception {
		var resource = getClass().getClassLoader().getResource(resourceName);
		assertTrue(resource != null, "Missing classpath resource: " + resourceName);
		return Path.of(resource.toURI());
	}

	private Set<String> procedureNames() throws IOException {
		Path procedureDirectory;
		try {
			procedureDirectory = resourcePath("portfolio-data/procedures");
		}
		catch (Exception exception) {
			throw new IOException("Could not locate procedure fixtures", exception);
		}

		Set<String> names = new HashSet<>();
		try (Stream<Path> files = Files.list(procedureDirectory)) {
			files.filter(path -> path.getFileName().toString().endsWith(".sql"))
					.map(path -> path.getFileName().toString().replaceFirst("\\.sql$", ""))
					.map(name -> name.replaceFirst("^dbo\\.", "dbo."))
					.forEach(names::add);
		}
		return names;
	}
}
