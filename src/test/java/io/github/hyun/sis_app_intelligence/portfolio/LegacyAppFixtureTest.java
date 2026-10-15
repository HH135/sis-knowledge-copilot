package io.github.hyun.sis_app_intelligence.portfolio;

import org.junit.jupiter.api.Test;

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

class LegacyAppFixtureTest {

    private static final Pattern PROCEDURE_NAME = Pattern.compile("dbo\\.(USP_[A-Z_]+)");
    private static final Pattern FORM_ACTION = Pattern.compile("action=\"([^\"]+)\"");
    private static final Pattern CONTROLLER_MAPPING =
            Pattern.compile("@RequestMapping\\(value = \"([^\"]+)\"");

    @Test
    void hasExactFixtureFileCounts() throws Exception {
        Path app = resourcePath("portfolio-data/app");
        assertEquals(20, countFiles(app.resolve("java"), ".java"));
        assertEquals(8, countFiles(app.resolve("webapp"), ".jsp"));
        assertEquals(6, countFiles(app.resolve("resources/mapper"), ".xml"));
        assertEquals(2, countFiles(app.resolve("resources/config"), null));
        assertEquals(1, countFiles(app.resolve("resources/config"), ".properties"));
        assertEquals(1, countFiles(app.resolve("resources/config"), ".ini"));
    }

    @Test
    void allMapperAndTraceProceduresHaveFixtureFiles() throws Exception {
        Path app = resourcePath("portfolio-data/app");
        Set<String> procedureFiles = new HashSet<>();
        Path procedures = resourcePath("portfolio-data/procedures");
        try (Stream<Path> files = Files.list(procedures)) {
            files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(path -> path.getFileName().toString().replaceFirst("^dbo\\.", "")
                            .replaceFirst("\\.sql$", ""))
                    .forEach(procedureFiles::add);
        }

        try (Stream<Path> mapperFiles = Files.list(app.resolve("resources/mapper"))) {
            for (Path mapper : mapperFiles.filter(path -> path.toString().endsWith(".xml")).toList()) {
                assertProcedureReferencesExist(Files.readString(mapper, StandardCharsets.UTF_8), procedureFiles,
                        mapper.getFileName().toString());
            }
        }

        String expectedTrace = Files.readString(app.resolve("EXPECTED_TRACE.md"), StandardCharsets.UTF_8);
        assertProcedureReferencesExist(expectedTrace, procedureFiles, "EXPECTED_TRACE.md");
        assertTrue(expectedTrace.contains("CommonMapper.retryInterfaceLog"));
    }

    @Test
    void jspActionsMatchControllerRequestMappings() throws Exception {
        Path app = resourcePath("portfolio-data/app");
        Set<String> formActions = new HashSet<>();
        try (Stream<Path> jspFiles = Files.walk(app.resolve("webapp"))) {
            for (Path jsp : jspFiles.filter(path -> path.toString().endsWith(".jsp")).toList()) {
                Matcher matcher = FORM_ACTION.matcher(Files.readString(jsp, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    formActions.add(matcher.group(1));
                }
            }
        }

        Set<String> controllerMappings = new HashSet<>();
        try (Stream<Path> javaFiles = Files.walk(app.resolve("java"))) {
            for (Path java : javaFiles.filter(path -> path.toString().endsWith("Controller.java")).toList()) {
                Matcher matcher = CONTROLLER_MAPPING.matcher(Files.readString(java, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    controllerMappings.add(matcher.group(1));
                }
            }
        }
        assertEquals(formActions, controllerMappings);
    }

    @Test
    void fixtureSourcesAreOutsideApplicationJavaSourceTree() throws Exception {
        Path appSources = Path.of("src", "main", "java");
        assertFalse(Files.exists(appSources.resolve("com/acme")));
    }

    private long countFiles(Path root, String extension) throws Exception {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> extension == null || path.getFileName().toString().endsWith(extension))
                    .count();
        }
    }

    private void assertProcedureReferencesExist(String content, Set<String> procedureFiles, String source) {
        Matcher matcher = PROCEDURE_NAME.matcher(content);
        while (matcher.find()) {
            assertTrue(procedureFiles.contains(matcher.group(1)),
                    "Missing procedure file for " + matcher.group(1) + " referenced by " + source);
        }
    }

    private Path resourcePath(String resourceName) throws Exception {
        var resource = getClass().getClassLoader().getResource(resourceName);
        assertTrue(resource != null, "Missing classpath resource: " + resourceName);
        return Path.of(resource.toURI());
    }
}
