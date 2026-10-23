package io.github.hyun.sis_app_intelligence.collector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;
import io.github.hyun.sis_app_intelligence.source.SourceFile;

@Component
@Profile("portfolio")
public class ClasspathSourceFileProvider implements SourceFileProvider {

    private static final List<String> PATTERNS = List.of(
            "classpath:portfolio-data/app/**",
            "classpath:portfolio-data/procedures/*.sql");

    private final ResourcePatternResolver resourceResolver;
    private final SourceProperties properties;
    private final CharsetResolver charsetResolver;

    public ClasspathSourceFileProvider(
            ResourcePatternResolver resourceResolver,
            SourceProperties properties,
            CharsetResolver charsetResolver) {
        this.resourceResolver = resourceResolver;
        this.properties = properties;
        this.charsetResolver = charsetResolver;
    }

    @Override
    public SourceFileBatch scan() {
        List<SourceFile> files = new ArrayList<>();
        Map<String, Integer> skipped = new HashMap<>();
        Map<String, Integer> byExtension = new HashMap<>();
        try {
            for (String pattern : PATTERNS) {
                for (Resource resource : resourceResolver.getResources(pattern)) {
                    String relativePath = relativePath(resource);
                    String extension = extension(relativePath);
                    if (!properties.getAllowedExtensions().contains(extension)
                            || isExcluded(relativePath)) {
                        continue;
                    }
                    long size = resource.contentLength();
                    if (size > properties.getMaxFileBytes()) {
                        skipped.merge("TOO_LARGE", 1, Integer::sum);
                        continue;
                    }
                    byte[] bytes;
                    try (InputStream input = resource.getInputStream()) {
                        bytes = input.readAllBytes();
                    }
                    if (bytes.length > properties.getMaxFileBytes()) {
                        skipped.merge("TOO_LARGE", 1, Integer::sum);
                        continue;
                    }
                    files.add(SourceFileSupport.create(
                            "portfolio", relativePath, extension, bytes, charsetResolver));
                    byExtension.merge(extension, 1, Integer::sum);
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException("Could not read portfolio source fixtures.");
        }
        return new SourceFileBatch("portfolio", List.copyOf(files), Map.copyOf(skipped),
                Map.copyOf(byExtension));
    }

    private String relativePath(Resource resource) throws IOException {
        String external = resource.getURL().toExternalForm().replace('\\', '/');
        int marker = external.lastIndexOf("portfolio-data/");
        if (marker < 0) {
            throw new IOException("Portfolio resource is outside the configured fixture root.");
        }
        return external.substring(marker);
    }

    private String extension(String relativePath) {
        int dot = relativePath.lastIndexOf('.');
        return dot < 0 ? "" : relativePath.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isExcluded(String relativePath) {
        for (String segment : relativePath.split("/")) {
            if (properties.getExcludedDirs().contains(segment)) {
                return true;
            }
        }
        return false;
    }
}
