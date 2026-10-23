package io.github.hyun.sis_app_intelligence.document;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.github.hyun.sis_app_intelligence.ai.EmbeddingProviderConfiguration;
import io.github.hyun.sis_app_intelligence.ai.MockEmbeddingAdapter;
import io.github.hyun.sis_app_intelligence.config.AiProperties;
import io.github.hyun.sis_app_intelligence.config.EmbeddingProperties;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmbeddingProfileTest {

    @Test
    void embeddingRoutesAreNotRegisteredForInternalProfile() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().setActiveProfiles("internal");
            context.register(InternalWebConfiguration.class);
            context.refresh();

            MockMvcBuilders.webAppContextSetup(context).build()
                    .perform(post("/api/embeddings/rebuild"))
                    .andExpect(status().isNotFound());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableConfigurationProperties({AiProperties.class, EmbeddingProperties.class, GeminiProperties.class})
    @Import({EmbeddingProviderConfiguration.class, MockEmbeddingAdapter.class, EmbeddingController.class})
    static class InternalWebConfiguration {
    }
}
