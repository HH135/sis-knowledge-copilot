package io.github.hyun.sis_app_intelligence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public class AiProperties {

    private String provider = "mock";
    private boolean externalEnabled;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        if (!"mock".equalsIgnoreCase(provider) && !"gemini".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "Unsupported app.ai.provider '" + provider + "'. Supported values are 'mock' and 'gemini'.");
        }
        this.provider = provider.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isExternalEnabled() {
        return externalEnabled;
    }

    public void setExternalEnabled(boolean externalEnabled) {
        this.externalEnabled = externalEnabled;
    }
}
