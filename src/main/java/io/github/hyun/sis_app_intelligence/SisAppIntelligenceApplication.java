package io.github.hyun.sis_app_intelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;
import io.github.hyun.sis_app_intelligence.config.MssqlProperties;

@SpringBootApplication
@EnableConfigurationProperties({SourceProperties.class, MssqlProperties.class})
public class SisAppIntelligenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SisAppIntelligenceApplication.class, args);
	}

}
