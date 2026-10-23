package io.github.hyun.sis_app_intelligence.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.source")
public class SourceProperties {

    private String root;
    private Set<String> allowedExtensions = new LinkedHashSet<>(
            Set.of("java", "jsp", "xml", "sql", "properties", "ini"));
    private Set<String> excludedDirs = new LinkedHashSet<>(
            Set.of(".git", "target", "node_modules", ".svn"));
    private long maxFileBytes = 2_097_152;
    private String fallbackCharset = "MS949";
    private Map<String, String> charsetOverrides = new LinkedHashMap<>();

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Set<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Set<String> getExcludedDirs() {
        return excludedDirs;
    }

    public void setExcludedDirs(Set<String> excludedDirs) {
        this.excludedDirs = excludedDirs;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }

    public String getFallbackCharset() {
        return fallbackCharset;
    }

    public void setFallbackCharset(String fallbackCharset) {
        this.fallbackCharset = fallbackCharset;
    }

    public Map<String, String> getCharsetOverrides() {
        return charsetOverrides;
    }

    public void setCharsetOverrides(Map<String, String> charsetOverrides) {
        this.charsetOverrides = charsetOverrides;
    }
}
