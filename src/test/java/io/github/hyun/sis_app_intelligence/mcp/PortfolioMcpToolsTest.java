package io.github.hyun.sis_app_intelligence.mcp;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.mcp.annotation.McpTool;

import io.github.hyun.sis_app_intelligence.dependency.CallTreeNode;
import io.github.hyun.sis_app_intelligence.dependency.DependencyService;
import io.github.hyun.sis_app_intelligence.metadata.ColumnSearchItem;
import io.github.hyun.sis_app_intelligence.metadata.MetadataSearchService;
import io.github.hyun.sis_app_intelligence.metadata.ProcedureSource;
import io.github.hyun.sis_app_intelligence.metadata.SearchResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PortfolioMcpToolsTest {

    private final MetadataSearchService metadataSearchService = mock(MetadataSearchService.class);
    private final DependencyService dependencyService = mock(DependencyService.class);
    private final PortfolioMcpTools tools = new PortfolioMcpTools(metadataSearchService, dependencyService);

    @Test
    void registersOnlyTheThreePortfolioReadTools() {
        Set<String> toolNames = java.util.Arrays.stream(PortfolioMcpTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(java.util.Objects::nonNull)
                .map(McpTool::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of("search_columns", "get_procedure_source", "get_call_tree"), toolNames);
        assertEquals("portfolio", PortfolioMcpTools.class.getAnnotation(Profile.class).value()[0]);
    }

    @Test
    void delegatesColumnSearchToTheValidatedMetadataService() {
        SearchResponse<ColumnSearchItem> expected = new SearchResponse<>("ORDER_NO", List.of());
        when(metadataSearchService.searchColumns("ORDER_NO", "TB_REPAIR_ORDER", 20)).thenReturn(expected);

        assertEquals(expected, tools.searchColumns("ORDER_NO", "TB_REPAIR_ORDER", 20));
        verify(metadataSearchService).searchColumns("ORDER_NO", "TB_REPAIR_ORDER", 20);
    }

    @Test
    void readsOnlyAnExactProcedureAndRejectsInvalidIdentifiers() {
        ProcedureSource expected = new ProcedureSource(
                "dbo", "USP_SAMPLE", "Fictional procedure", "portfolio-data/procedures/dbo.USP_SAMPLE.sql",
                "OK", "CREATE PROCEDURE dbo.USP_SAMPLE AS SELECT 1;");
        when(metadataSearchService.findProcedureSource("dbo", "USP_SAMPLE")).thenReturn(Optional.of(expected));

        assertEquals(expected, tools.getProcedureSource("dbo", "USP_SAMPLE"));
        assertThrows(IllegalArgumentException.class, () -> tools.getProcedureSource("dbo", "USP_SAMPLE;DROP"));
        verify(metadataSearchService).findProcedureSource("dbo", "USP_SAMPLE");
    }

    @Test
    void appliesDefaultAndStrictBoundsToCallTreeDepth() {
        CallTreeNode expected = new CallTreeNode("dbo.USP_SAMPLE", "ROOT", null, false, List.of());
        when(dependencyService.callTree("dbo.USP_SAMPLE", 5)).thenReturn(expected);
        assertEquals(expected, tools.getCallTree("dbo.USP_SAMPLE", null));

        assertThrows(IllegalArgumentException.class, () -> tools.getCallTree("dbo.USP_SAMPLE", 11));
        assertThrows(IllegalArgumentException.class, () -> tools.getCallTree("dbo.USP_SAMPLE.extra", 2));
        verify(dependencyService).callTree("dbo.USP_SAMPLE", 5);
        verifyNoInteractions(metadataSearchService);
    }
}
