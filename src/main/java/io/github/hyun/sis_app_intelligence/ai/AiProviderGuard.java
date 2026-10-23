package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;

import io.github.hyun.sis_app_intelligence.config.AiProperties;

public class AiProviderGuard implements SmartInitializingSingleton {

    private final Environment environment;
    private final AiProperties aiProperties;

    public AiProviderGuard(Environment environment, AiProperties aiProperties) {
        this.environment = environment;
        this.aiProperties = aiProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!"gemini".equals(aiProperties.getProvider())) {
            return;
        }
        if (environment.matchesProfiles("internal")) {
            throw new IllegalStateException("Gemini analysis is prohibited in the internal profile.");
        }
        if (!environment.matchesProfiles("portfolio")) {
            throw new IllegalStateException("Gemini analysis is available only in the portfolio profile.");
        }
        if (!aiProperties.isExternalEnabled()) {
            throw new IllegalStateException("Gemini analysis requires app.ai.external-enabled=true.");
        }
        if (environment.getProperty("app.ai.gemini.api-key", "").isBlank()) {
            throw new IllegalStateException("app.ai.gemini.api-key must be configured when provider is gemini.");
        }
    }
}
