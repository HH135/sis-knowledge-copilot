package io.github.hyun.sis_app_intelligence.metadata;

public class MetadataSourceNotConfiguredException extends RuntimeException {
    public MetadataSourceNotConfiguredException() {
        super("No metadata source is configured for the active profile.");
    }
}
