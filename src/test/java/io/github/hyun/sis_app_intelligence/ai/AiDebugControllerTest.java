package io.github.hyun.sis_app_intelligence.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import io.github.hyun.sis_app_intelligence.config.AiProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class AiDebugControllerTest {

    private AnnotationConfigApplicationContext context;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("portfolio");
        context.register(PortfolioAiConfiguration.class);
        context.refresh();
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(context.getBean(AiDebugController.class))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
        context.close();
    }

    @Test
    void portfolioEndpointReturnsAnalysisAndCitations() throws Exception {
        mockMvc.perform(post("/api/ai/analyze-debug")
                        .contentType("application/json")
                        .content("""
                                {"question":"Why?","contexts":[
                                  {"sourceKind":"source","sourceRef":"src/Example.java","title":"Example","startLine":1,"endLine":5,"content":"text"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("[MOCK] Why? + Example"))
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.citations[0].sourceRef").value("src/Example.java"));
    }

    @Test
    void rejectsMissingQuestionLongQuestionAndMoreThanTenContexts() throws Exception {
        mockMvc.perform(post("/api/ai/analyze-debug")
                        .contentType("application/json")
                        .content("{\"contexts\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/ai/analyze-debug")
                        .contentType("application/json")
                        .content("{\"question\":\"  \",\"contexts\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/ai/analyze-debug")
                        .contentType("application/json")
                        .content("{\"question\":\"" + "q".repeat(501) + "\",\"contexts\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/ai/analyze-debug")
                        .contentType("application/json")
                        .content("{\"question\":\"q\",\"contexts\":" + contextsJson(11) + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debugControllerReturnsNotFoundInInternalProfile() throws Exception {
        try (AnnotationConfigWebApplicationContext internal = new AnnotationConfigWebApplicationContext()) {
            internal.setServletContext(new MockServletContext());
            internal.getEnvironment().setActiveProfiles("internal");
            internal.register(InternalAiConfiguration.class);
            internal.refresh();
            assertFalse(internal.getBeansOfType(AiDebugController.class).size() > 0);
            webAppContextSetup(internal).build()
                    .perform(post("/api/ai/analyze-debug")
                            .contentType("application/json")
                            .content("{\"question\":\"q\",\"contexts\":[]}"))
                    .andExpect(status().isNotFound());
        }
        assertTrue(context.getBeansOfType(AiDebugController.class).size() > 0);
    }

    private String contextsJson(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> "{\"sourceKind\":\"source\",\"sourceRef\":\"ref-" + index
                        + "\",\"title\":\"title-" + index + "\",\"content\":\"text\"}")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiConfig.class, MockAiAnalysisAdapter.class, AiDebugController.class})
    static class PortfolioAiConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiConfig.class, MockAiAnalysisAdapter.class, AiDebugController.class})
    static class InternalAiConfiguration {
    }
}
