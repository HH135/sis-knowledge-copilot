package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.EmbeddingProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

@Configuration(proxyBeanMethods = false)
public class EmbeddingSecurityConfiguration {

    @Bean
    EmbeddingSecurityGuard embeddingSecurityGuard(
            Environment environment,
            ObjectProvider<EmbeddingPort> embeddingPortProvider,
            AiProperties aiProperties,
            EmbeddingProperties embeddingProperties,
            GeminiProperties geminiProperties) {
        return new EmbeddingSecurityGuard(
                environment, embeddingPortProvider, aiProperties, embeddingProperties, geminiProperties);
    }
}
