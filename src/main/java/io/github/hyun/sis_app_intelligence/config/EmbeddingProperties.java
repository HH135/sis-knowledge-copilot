package io.github.hyun.sis_app_intelligence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai.embedding")
public class EmbeddingProperties {

    private String provider = "mock";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        if (!"mock".equalsIgnoreCase(provider) && !"gemini".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "Unsupported app.ai.embedding.provider '" + provider + "'. Supported values are 'mock' and 'gemini'.");
        }
        this.provider = provider.toLowerCase();
    }
}
