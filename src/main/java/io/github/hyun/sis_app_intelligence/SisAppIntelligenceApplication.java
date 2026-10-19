package io.github.hyun.sis_app_intelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;
import io.github.hyun.sis_app_intelligence.config.MssqlProperties;
import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.EmbeddingProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		SourceProperties.class,
		MssqlProperties.class,
		AiProperties.class,
		EmbeddingProperties.class,
		GeminiProperties.class
})
public class SisAppIntelligenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SisAppIntelligenceApplication.class, args);
	}

}
