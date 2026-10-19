package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;

import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.EmbeddingProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

public class EmbeddingSecurityGuard implements SmartInitializingSingleton {

    private final Environment environment;
    private final ObjectProvider<EmbeddingPort> embeddingPortProvider;
    private final AiProperties aiProperties;
    private final EmbeddingProperties embeddingProperties;
    private final GeminiProperties geminiProperties;

    public EmbeddingSecurityGuard(
            Environment environment,
            ObjectProvider<EmbeddingPort> embeddingPortProvider,
            AiProperties aiProperties,
            EmbeddingProperties embeddingProperties,
            GeminiProperties geminiProperties) {
        this.environment = environment;
        this.embeddingPortProvider = embeddingPortProvider;
        this.aiProperties = aiProperties;
        this.embeddingProperties = embeddingProperties;
        this.geminiProperties = geminiProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String provider = embeddingProperties.getProvider();
        if ("gemini".equals(provider)) {
            if (environment.matchesProfiles("internal")) {
                throw new IllegalStateException("External embedding providers are prohibited in the internal profile.");
            }
            if (!environment.matchesProfiles("portfolio")) {
                throw new IllegalStateException("Gemini embeddings are available only in the portfolio profile.");
            }
            if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
                throw new IllegalStateException("app.ai.gemini.api-key must be configured when provider is gemini.");
            }
            if (!aiProperties.isExternalEnabled()) {
                throw new IllegalStateException("Gemini embeddings require app.ai.external-enabled=true.");
            }
        }

        boolean externalEmbeddingPortPresent = embeddingPortProvider.stream()
                .anyMatch(EmbeddingPort::sendsDataExternally);
        if (externalEmbeddingPortPresent) {
            if (environment.matchesProfiles("internal")) {
                throw new IllegalStateException("External embedding providers are prohibited in the internal profile.");
            }
            if (!aiProperties.isExternalEnabled()) {
                throw new IllegalStateException("External embedding providers require app.ai.external-enabled=true.");
            }
        }
    }
}
