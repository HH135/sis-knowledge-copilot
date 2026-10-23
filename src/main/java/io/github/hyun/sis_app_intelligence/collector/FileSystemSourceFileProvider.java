package io.github.hyun.sis_app_intelligence.collector;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;
import io.github.hyun.sis_app_intelligence.source.SourceFile;

@Component
@Profile("internal")
public class FileSystemSourceFileProvider implements SourceFileProvider {

    private final SourceProperties properties;
    private final CharsetResolver charsetResolver;

    public FileSystemSourceFileProvider(SourceProperties properties, CharsetResolver charsetResolver) {
        this.properties = properties;
        this.charsetResolver = charsetResolver;
    }

    @Override
    public SourceFileBatch scan() {
        String configuredRoot = properties.getRoot();
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new SourceRootNotConfiguredException();
        }

        Path root = Path.of(configuredRoot).toAbsolutePath().normalize();
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw new SourceRootNotFoundException();
        }

        List<SourceFile> files = new ArrayList<>();
        Map<String, Integer> skipped = new HashMap<>();
        Map<String, Integer> byExtension = new HashMap<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (!directory.equals(root) && isExcludedDirectory(directory)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    Path normalized = file.toAbsolutePath().normalize();
                    if (!normalized.startsWith(root)) {
                        skipped.merge("OUTSIDE_ROOT", 1, Integer::sum);
                        return FileVisitResult.CONTINUE;
                    }
                    if (Files.isSymbolicLink(file)) {
                        skipped.merge(isOutsideRootLink(file, root) ? "OUTSIDE_ROOT" : "SYMLINK",
                                1, Integer::sum);
                        return FileVisitResult.CONTINUE;
                    }
                    if (!attributes.isRegularFile()) {
                        return FileVisitResult.CONTINUE;
                    }

                    String relativePath = root.relativize(normalized).toString().replace('\\', '/');
                    String extension = extension(relativePath);
                    if (!properties.getAllowedExtensions().contains(extension)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (attributes.size() > properties.getMaxFileBytes()) {
                        skipped.merge("TOO_LARGE", 1, Integer::sum);
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        byte[] bytes = Files.readAllBytes(file);
                        if (bytes.length > properties.getMaxFileBytes()) {
                            skipped.merge("TOO_LARGE", 1, Integer::sum);
                            return FileVisitResult.CONTINUE;
                        }
                        files.add(SourceFileSupport.create(
                                "internal", relativePath, extension, bytes, charsetResolver));
                        byExtension.merge(extension, 1, Integer::sum);
                    }
                    catch (IOException exception) {
                        throw new SourceScanIOException(relativePath, exception);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    throw new SourceScanIOException(root.relativize(file.toAbsolutePath().normalize())
                            .toString().replace('\\', '/'), exception);
                }

                private boolean isExcludedDirectory(Path directory) {
                    return properties.getExcludedDirs().contains(directory.getFileName().toString());
                }
            });
        }
        catch (SourceScanIOException exception) {
            throw new IllegalStateException("Could not scan source file: " + exception.relativePath());
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not scan configured source root.");
        }
        return new SourceFileBatch("internal", List.copyOf(files), Map.copyOf(skipped),
                Map.copyOf(byExtension));
    }

    private boolean isOutsideRootLink(Path link, Path root) {
        try {
            Path target = Files.readSymbolicLink(link);
            Path normalizedTarget = target.isAbsolute()
                    ? target.normalize()
                    : link.getParent().resolve(target).normalize();
            return !normalizedTarget.startsWith(root);
        }
        catch (IOException exception) {
            return true;
        }
    }

    private String extension(String relativePath) {
        int dot = relativePath.lastIndexOf('.');
        return dot < 0 ? "" : relativePath.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static final class SourceScanIOException extends RuntimeException {
        private final String relativePath;

        private SourceScanIOException(String relativePath, IOException cause) {
            super(cause);
            this.relativePath = relativePath;
        }

        private String relativePath() {
            return relativePath;
        }
    }
}
