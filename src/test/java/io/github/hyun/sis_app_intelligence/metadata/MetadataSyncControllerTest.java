package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MetadataSyncControllerTest {

    @Test
    void portfolioSourceReturnsSyncResultJson() throws Exception {
        MetadataSourcePort source = mock(MetadataSourcePort.class);
        MetadataRepository repository = mock(MetadataRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MetadataSourcePort> sourceProvider = mock(ObjectProvider.class);
        when(sourceProvider.getIfAvailable()).thenReturn(source);
        when(source.load()).thenReturn(new MetadataSnapshot(
                "ACME_SVC",
                List.of(new TableMeta("dbo", "TB_CUSTOMER", "고객",
                        List.of(new ColumnMeta("CUST_NO", 1, "varchar(20)", false, "고객번호")))),
                List.of(new ProcedureMeta("dbo", "USP_SAMPLE", "샘플", "CREATE PROCEDURE ...",
                        "portfolio-data/procedures/dbo.USP_SAMPLE.sql", "OK"))));
        MockMvc mockMvc = standaloneSetup(new MetadataSyncController(
                new MetadataSyncService(sourceProvider, repository))).build();

        mockMvc.perform(post("/api/metadata/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databaseName").value("ACME_SVC"))
                .andExpect(jsonPath("$.tableCount").value(1))
                .andExpect(jsonPath("$.columnCount").value(1))
                .andExpect(jsonPath("$.procedureCount").value(1));
    }

    @Test
    void returnsServiceUnavailableWhenNoSourceAdapterExists() throws Exception {
        MetadataRepository repository = mock(MetadataRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MetadataSourcePort> sourceProvider = mock(ObjectProvider.class);
        when(sourceProvider.getIfAvailable()).thenReturn(null);
        MockMvc mockMvc = standaloneSetup(new MetadataSyncController(
                new MetadataSyncService(sourceProvider, repository))).build();

        mockMvc.perform(post("/api/metadata/sync"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("METADATA_SOURCE_NOT_CONFIGURED"));
    }
}
