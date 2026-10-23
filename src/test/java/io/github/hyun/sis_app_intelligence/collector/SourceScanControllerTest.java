package io.github.hyun.sis_app_intelligence.collector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import io.github.hyun.sis_app_intelligence.config.SourceProperties;
import io.github.hyun.sis_app_intelligence.source.SourceFileRepository;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SourceScanControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsServiceUnavailableWhenInternalRootIsNotConfigured() throws Exception {
        MockMvc mockMvc = mockMvcFor(new SourceProperties());

        mockMvc.perform(post("/api/source-files/scan"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SOURCE_ROOT_NOT_CONFIGURED"));
    }

    @Test
    void returnsBadRequestWhenInternalRootDoesNotExist() throws Exception {
        SourceProperties properties = new SourceProperties();
        properties.setRoot(tempDir.resolve("missing").toString());
        MockMvc mockMvc = mockMvcFor(properties);

        mockMvc.perform(post("/api/source-files/scan"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SOURCE_ROOT_NOT_FOUND"));
    }

    @SuppressWarnings("unchecked")
    private MockMvc mockMvcFor(SourceProperties properties) {
        ObjectProvider<SourceFileProvider> provider = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable())
                .thenReturn(new FileSystemSourceFileProvider(properties, new CharsetResolver(properties)));
        SourceFileRepository repository = new SourceFileRepository(mock(JdbcTemplate.class));
        SourceCollectorService service = new SourceCollectorService(provider, repository);
        return standaloneSetup(new SourceScanController(service)).build();
    }
}
