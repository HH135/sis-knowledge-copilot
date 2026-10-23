package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({EmbeddingSecurityConfiguration.class, GeminiEmbeddingConfiguration.class})
public class EmbeddingProviderConfiguration {
}
