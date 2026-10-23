package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import io.github.hyun.sis_app_intelligence.config.MssqlProperties;
import io.github.hyun.sis_app_intelligence.source.SourceFileRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MssqlSyncSafetyTest {

    @Test
    void internalProfileContextStartsWithoutMssqlAndKeepsPrimaryPostgresDataSource() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("internal");
            context.register(InternalTestConfiguration.class);
            context.refresh();

            DriverManagerDataSource dataSource = context.getBean(DriverManagerDataSource.class);
            assertEquals("jdbc:postgresql:unit_test", dataSource.getUrl());
            assertEquals(dataSource, context.getBean(JdbcTemplate.class).getDataSource());

            MetadataSyncController controller = context.getBean(MetadataSyncController.class);
            MockMvc mockMvc = standaloneSetup(controller).build();
            mockMvc.perform(post("/api/metadata/sync"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("MSSQL_NOT_CONFIGURED"));
        }
    }

    @Test
    void unavailableMssqlReturnsGatewayErrorAndDoesNotReplaceExistingMetadata() throws Exception {
        MetadataSourcePort source = mock(MetadataSourcePort.class);
        MetadataRepository repository = mock(MetadataRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MetadataSourcePort> sourceProvider = mock(ObjectProvider.class);
        when(sourceProvider.getIfAvailable()).thenReturn(source);
        when(source.load()).thenThrow(new MssqlUnavailableException());
        MetadataSyncController controller = new MetadataSyncController(
                new MetadataSyncService(sourceProvider, repository));
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(post("/api/metadata/sync"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("MSSQL_UNAVAILABLE"));
        verify(repository, never()).replace(org.mockito.ArgumentMatchers.any());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MssqlProperties.class)
    @Import({
            MssqlMetadataSource.class,
            MetadataRepository.class,
            MetadataSyncService.class,
            MetadataSyncController.class
    })
    static class InternalTestConfiguration {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl("jdbc:postgresql:unit_test");
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
