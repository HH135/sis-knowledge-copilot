package io.github.hyun.sis_app_intelligence.collector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemSourceFileProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void scansOnlyAllowedExtensionsAndSkipsExcludedAndOversizedFiles() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path excluded = Files.createDirectories(root.resolve(".git"));
        Files.writeString(root.resolve("ReadMe.JAVA"), "ok", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("ignored.txt"), "ignored", StandardCharsets.UTF_8);
        Files.writeString(excluded.resolve("ignored.java"), "ignored", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("large.sql"), "123456789", StandardCharsets.UTF_8);

        SourceProperties properties = properties(root);
        properties.setMaxFileBytes(8);
        FileSystemSourceFileProvider provider = provider(properties);

        SourceFileBatch batch = provider.scan();

        assertEquals(1, batch.files().size());
        assertEquals("ReadMe.JAVA", batch.files().getFirst().relativePath());
        assertEquals(1, batch.byExtension().get("java"));
        assertEquals(1, batch.skippedReasons().get("TOO_LARGE"));
        assertFalse(batch.files().stream().anyMatch(file -> file.relativePath().contains(".git")));
    }

    @Test
    void doesNotFollowSymlinksOutsideRoot() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path external = tempDir.resolve("external.java");
        Files.writeString(external, "class External {}", StandardCharsets.UTF_8);
        Path link = root.resolve("external.java");
        try {
            Files.createSymbolicLink(link, external);
        }
        catch (UnsupportedOperationException | IOException | SecurityException exception) {
            Assumptions.abort("Symbolic links are not available in this environment.");
        }

        SourceFileBatch batch = provider(properties(root)).scan();

        assertTrue(batch.files().isEmpty());
        assertEquals(1, batch.skippedReasons().get("OUTSIDE_ROOT"));
    }

    @Test
    void rejectsMissingRootWithoutReturningItsPath() {
        SourceProperties properties = properties(tempDir.resolve("missing"));

        SourceRootNotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
                SourceRootNotFoundException.class, () -> provider(properties).scan());

        assertFalse(exception.getMessage() != null && exception.getMessage().contains(tempDir.toString()));
    }

    private SourceProperties properties(Path root) {
        SourceProperties properties = new SourceProperties();
        properties.setRoot(root.toString());
        properties.setExcludedDirs(Set.of(".git", "target", "node_modules", ".svn"));
        return properties;
    }

    private FileSystemSourceFileProvider provider(SourceProperties properties) {
        return new FileSystemSourceFileProvider(properties, new CharsetResolver(properties));
    }
}
