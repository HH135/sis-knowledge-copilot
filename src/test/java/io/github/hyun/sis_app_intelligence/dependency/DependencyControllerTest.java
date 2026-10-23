package io.github.hyun.sis_app_intelligence.dependency;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DependencyControllerTest {

    @Test
    void servesTreeAndCallersAndNotFound() throws Exception {
        DependencyService service = mock(DependencyService.class);
        when(service.callTree("dbo.USP_SAMPLE", 5)).thenReturn(new CallTreeNode(
                "dbo.USP_SAMPLE", "ROOT", null, false,
                List.of(new CallTreeNode("dbo.USP_CHILD", "RESOLVED", 12, false, List.of()))));
        when(service.callers("dbo.USP_SAMPLE"))
                .thenReturn(List.of(new CallerItem("MAPPER_STATEMENT", "mapper.xml#find", "RESOLVED", 4)));
        when(service.callTree("unknown", 5)).thenThrow(new ProcedureNotFoundException());
        DependencyController controller = new DependencyController(service);
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/api/dependencies/call-tree").param("procedure", "dbo.USP_SAMPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.children[0].procedure").value("dbo.USP_CHILD"));
        mockMvc.perform(get("/api/dependencies/callers").param("procedure", "dbo.USP_SAMPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].callerType").value("MAPPER_STATEMENT"));
        mockMvc.perform(get("/api/dependencies/call-tree").param("procedure", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PROCEDURE_NOT_FOUND"));
    }

    @Test
    void depthIsCappedAndRebuildReturnsCounts() throws Exception {
        DependencyService service = mock(DependencyService.class);
        when(service.callTree("dbo.USP_SAMPLE", 10)).thenReturn(
                new CallTreeNode("dbo.USP_SAMPLE", "ROOT", null, false, List.of()));
        when(service.rebuild()).thenReturn(new DependencyRebuildResult(
                "portfolio", 10, Map.of("RESOLVED", 8, "MISSING", 1, "DYNAMIC", 1, "SYSTEM", 0), 2));
        MockMvc mockMvc = standaloneSetup(new DependencyController(service)).build();

        mockMvc.perform(get("/api/dependencies/call-tree")
                .param("procedure", "dbo.USP_SAMPLE").param("depth", "20"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/dependencies/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byType.RESOLVED").value(8))
                .andExpect(jsonPath("$.mapperEdges").value(2));
    }
}
