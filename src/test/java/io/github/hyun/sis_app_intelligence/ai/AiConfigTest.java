package io.github.hyun.sis_app_intelligence.ai;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigTest {

    @Test
    void internalProfileUsesNonExternalMockAdapter() {
        try (AnnotationConfigApplicationContext context = context(InternalAiConfiguration.class, Map.of())) {
            AiAnalysisPort adapter = context.getBean(AiAnalysisPort.class);

            assertEquals("mock", adapter.providerName());
            assertFalse(adapter.sendsDataExternally());
        }
    }

    @Test
    void internalProfileRejectsAnExternalAdapterAtStartup() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("internal");
            context.register(ExternalAiConfiguration.class);

            IllegalStateException exception = assertThrows(IllegalStateException.class, context::refresh);
            assertTrue(exception.getMessage().contains("prohibited"));
        }
    }

    @Test
    void portfolioProfileRequiresExplicitExternalOptIn() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("portfolio");
            context.register(ExternalAiConfiguration.class);
            IllegalStateException exception = assertThrows(IllegalStateException.class, context::refresh);
            assertTrue(exception.getMessage().contains("app.ai.external-enabled=true"));
        }

        try (AnnotationConfigApplicationContext context = context(
                ExternalAiConfiguration.class, Map.of("app.ai.external-enabled", "true"), "portfolio")) {
            assertTrue(context.getBean(AiAnalysisPort.class).sendsDataExternally());
        }
    }

    @Test
    void unsupportedProviderFailsAtStartupWithClearMessage() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test", Map.of("app.ai.provider", "vendor")));
            context.register(InternalAiConfiguration.class);

            RuntimeException exception = assertThrows(RuntimeException.class, context::refresh);
            assertTrue(rootCause(exception).getMessage().contains(
                    "Unsupported app.ai.provider 'vendor'. Supported values are 'mock' and 'gemini'."));
        }
    }

    @Test
    void geminiProviderIsRejectedInInternalProfileAndWithoutPortfolioApiKey() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("internal");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test", Map.of("app.ai.provider", "gemini", "app.ai.external-enabled", "true")));
            context.register(GeminiGuardConfiguration.class);
            IllegalStateException exception = assertThrows(IllegalStateException.class, context::refresh);
            assertTrue(exception.getMessage().contains("prohibited"));
        }

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("portfolio");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test", Map.of("app.ai.provider", "gemini", "app.ai.external-enabled", "true")));
            context.register(GeminiGuardConfiguration.class);
            IllegalStateException exception = assertThrows(IllegalStateException.class, context::refresh);
            assertTrue(exception.getMessage().contains("api-key"));
        }
    }

    @Test
    void geminiAdapterIsSelectedOnlyWithPortfolioOptInAndKey() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("portfolio");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test", Map.of(
                            "app.ai.provider", "gemini",
                            "app.ai.external-enabled", "true",
                            "app.ai.gemini.api-key", "test-key")));
            context.register(GeminiProviderConfiguration.class);
            context.refresh();

            assertTrue(context.getBean(AiAnalysisPort.class).sendsDataExternally());
            assertEquals("gemini", context.getBean(AiAnalysisPort.class).providerName());
        }
    }

    private AnnotationConfigApplicationContext context(Class<?> configuration, Map<String, Object> properties) {
        return context(configuration, properties, "internal");
    }

    private AnnotationConfigApplicationContext context(
            Class<?> configuration, Map<String, Object> properties, String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.register(configuration);
        context.refresh();
        return context;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiConfig.class, MockAiAnalysisAdapter.class})
    static class InternalAiConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiConfig.class, MockAiAnalysisAdapter.class})
    static class ExternalAiConfiguration {

        @Bean
        @org.springframework.context.annotation.Primary
        AiAnalysisPort testExternalAdapter() {
            return new AiAnalysisPort() {
                @Override
                public AnalysisResult analyze(AnalysisRequest request) {
                    return new AnalysisResult("external", java.util.List.of(), "test-external");
                }

                @Override
                public String providerName() {
                    return "test-external";
                }

                @Override
                public boolean sendsDataExternally() {
                    return true;
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import(AiConfig.class)
    static class GeminiGuardConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AiProperties.class, GeminiProperties.class})
    @Import({AiConfig.class, GeminiAnalysisConfiguration.class})
    static class GeminiProviderConfiguration {
    }
}
