package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import io.github.hyun.sis_app_intelligence.config.AiProperties;

public class ExternalAiGuard implements SmartInitializingSingleton {

    private final Environment environment;
    private final ObjectProvider<AiAnalysisPort> aiAnalysisPort;
    private final AiProperties properties;

    public ExternalAiGuard(
            Environment environment,
            ObjectProvider<AiAnalysisPort> aiAnalysisPort,
            AiProperties properties) {
        this.environment = environment;
        this.aiAnalysisPort = aiAnalysisPort;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        AiAnalysisPort adapter = aiAnalysisPort.getIfAvailable();
        if (adapter == null || !adapter.sendsDataExternally()) {
            return;
        }
        if (environment.matchesProfiles("internal")) {
            throw new IllegalStateException("External AI adapters are prohibited in the internal profile.");
        }
        if (!properties.isExternalEnabled()) {
            throw new IllegalStateException(
                    "External AI adapters require app.ai.external-enabled=true.");
        }
    }
}
