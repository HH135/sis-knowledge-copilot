package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.EmbeddingProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddingSecurityGuardTest {

    @Test
    void rejectsGeminiProviderInInternalProfile() {
        IllegalStateException exception = refreshAndCapture("internal", Map.of(
                "app.ai.embedding.provider", "gemini",
                "app.ai.gemini.api-key", "test-key",
                "app.ai.external-enabled", "true"));

        assertTrue(exception.getMessage().contains("internal profile"));
    }

    @Test
    void rejectsGeminiProviderWithoutApiKey() {
        IllegalStateException exception = refreshAndCapture("portfolio", Map.of(
                "app.ai.embedding.provider", "gemini",
                "app.ai.external-enabled", "true",
                "app.ai.gemini.api-key", ""));

        assertTrue(exception.getMessage().contains("api-key"));
        assertTrue(!exception.getMessage().contains("test-key"));
    }

    @Test
    void rejectsInjectedExternalEmbeddingPortInInternalProfile() {
        IllegalStateException exception = refreshAndCapture(
                "internal",
                Map.of("app.ai.embedding.provider", "mock"),
                GuardConfiguration.ExternalEmbeddingGuardConfiguration.class);

        assertTrue(exception.getMessage().contains("internal profile"));
    }

    private IllegalStateException refreshAndCapture(String profile, Map<String, Object> properties) {
        return refreshAndCapture(profile, properties, GuardConfiguration.class);
    }

    private IllegalStateException refreshAndCapture(
            String profile,
            Map<String, Object> properties,
            Class<?> configurationClass) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profile);
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
            context.register(configurationClass);
            RuntimeException exception = assertThrows(RuntimeException.class, context::refresh);
            Throwable root = exception;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            return (IllegalStateException) root;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiProperties.class, EmbeddingProperties.class, GeminiProperties.class})
    @Import(EmbeddingSecurityConfiguration.class)
    static class GuardConfiguration {

        @Bean
        AiAnalysisPort aiAnalysisPort() {
            return new AiAnalysisPort() {
                @Override
                public AnalysisResult analyze(AnalysisRequest request) {
                    return new AnalysisResult("", List.of(), "mock");
                }

                @Override
                public String providerName() {
                    return "mock";
                }

                @Override
                public boolean sendsDataExternally() {
                    return false;
                }
            };
        }

        @Configuration(proxyBeanMethods = false)
        @EnableConfigurationProperties({AiProperties.class, EmbeddingProperties.class, GeminiProperties.class})
        @Import(EmbeddingSecurityConfiguration.class)
        static class ExternalEmbeddingGuardConfiguration {

            @Bean
            AiAnalysisPort aiAnalysisPort() {
                return new GuardConfiguration().aiAnalysisPort();
            }

            @Bean
            EmbeddingPort mockEmbeddingPort() {
                return new MockEmbeddingAdapter();
            }

            @Bean
            EmbeddingPort externalEmbeddingPort() {
                return new EmbeddingPort() {
                    @Override
                    public List<float[]> embedDocuments(List<String> texts) {
                        return List.of();
                    }

                    @Override
                    public float[] embedQuery(String text) {
                        return new float[0];
                    }

                    @Override
                    public String modelName() {
                        return "test";
                    }

                    @Override
                    public int dimensions() {
                        return 768;
                    }

                    @Override
                    public boolean sendsDataExternally() {
                        return true;
                    }
                };
            }
        }
    }
}
