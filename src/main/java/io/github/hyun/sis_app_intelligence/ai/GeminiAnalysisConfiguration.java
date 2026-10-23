package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

@Configuration(proxyBeanMethods = false)
@Profile("portfolio")
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiAnalysisConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.ai.external-enabled", havingValue = "true")
    GeminiAnalysisAdapter geminiAnalysisAdapter(GeminiProperties properties) {
        return new GeminiAnalysisAdapter(properties);
    }
}
