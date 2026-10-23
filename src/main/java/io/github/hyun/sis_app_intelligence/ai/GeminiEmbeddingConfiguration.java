package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.EmbeddingProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

@Configuration(proxyBeanMethods = false)
@Profile("portfolio")
@ConditionalOnProperty(name = "app.ai.embedding.provider", havingValue = "gemini")
@ConditionalOnProperty(name = "app.ai.external-enabled", havingValue = "true")
public class GeminiEmbeddingConfiguration {

    @Bean
    GeminiEmbeddingAdapter geminiEmbeddingAdapter(GeminiProperties properties) {
        return new GeminiEmbeddingAdapter(properties);
    }
}
