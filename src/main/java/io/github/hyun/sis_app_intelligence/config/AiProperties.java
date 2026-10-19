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
        if (!"mock".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "Unsupported app.ai.provider '" + provider + "'. Only 'mock' is currently supported.");
        }
        this.provider = provider;
    }

    public boolean isExternalEnabled() {
        return externalEnabled;
    }

    public void setExternalEnabled(boolean externalEnabled) {
        this.externalEnabled = externalEnabled;
    }
}
