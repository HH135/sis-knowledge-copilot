package io.github.hyun.sis_app_intelligence.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.github.hyun.sis_app_intelligence.config.AiProperties;
@Configuration(proxyBeanMethods = false)
public class AiConfig {

    @Bean
    ExternalAiGuard externalAiGuard(Environment environment, AiAnalysisPort aiAnalysisPort,
                                    AiProperties properties) {
        return new ExternalAiGuard(environment, aiAnalysisPort, properties);
    }
}
