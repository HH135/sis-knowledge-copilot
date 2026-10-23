package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.hyun.sis_app_intelligence.dependency.CallTreeNode;
import io.github.hyun.sis_app_intelligence.dependency.DependencyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DependencyExpanderTest {

    @Test
    void addsResolvedDescendantsAndDeduplicatesWhileKeepingSearchResultsFirst() {
        DependencyService dependencies = mock(DependencyService.class);
        RagRepository repository = mock(RagRepository.class);
        when(dependencies.callTree("dbo.USP_ROOT", 3)).thenReturn(new CallTreeNode(
                "dbo.USP_ROOT", "ROOT", null, false, List.of(
                        new CallTreeNode("dbo.USP_CHILD", "RESOLVED", 4, false, List.of(
                                new CallTreeNode("dbo.USP_GRANDCHILD", "RESOLVED", 7, false, List.of()))),
                        new CallTreeNode("dbo.USP_MISSING", "MISSING", 8, false, List.of()))));
        when(dependencies.callTree("dbo.USP_CHILD", 3)).thenReturn(new CallTreeNode(
                "dbo.USP_CHILD", "ROOT", null, false, List.of()));
        when(repository.findProcedureChunks("portfolio", "dbo.USP_CHILD"))
                .thenReturn(List.of(procedureChunk(2, "dbo.USP_CHILD")));
        when(repository.findProcedureChunks("portfolio", "dbo.USP_GRANDCHILD"))
                .thenReturn(List.of(procedureChunk(3, "dbo.USP_GRANDCHILD")));

        List<RetrievedChunk> expanded = new DependencyExpander(dependencies, repository).expand(
                "portfolio", List.of(procedureChunk(1, "dbo.USP_ROOT"), procedureChunk(2, "dbo.USP_CHILD")), true);

        assertEquals(List.of(1L, 2L, 3L), expanded.stream().map(RetrievedChunk::chunkId).toList());
        assertTrue(expanded.get(2).expanded());
        assertEquals("dbo.USP_ROOT", expanded.get(2).expandedFrom());
    }

    @Test
    void capsContextsAtTenWithSearchResultsBeforeExpansions() {
        DependencyService dependencies = mock(DependencyService.class);
        RagRepository repository = mock(RagRepository.class);
        when(dependencies.callTree("dbo.USP_ROOT", 3)).thenReturn(new CallTreeNode(
                "dbo.USP_ROOT", "ROOT", null, false,
                List.of(new CallTreeNode("dbo.USP_CHILD", "RESOLVED", null, false, List.of()))));
        when(repository.findProcedureChunks("portfolio", "dbo.USP_CHILD"))
                .thenReturn(List.of(procedureChunk(20, "dbo.USP_CHILD")));
        List<RetrievedChunk> results = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(id -> procedureChunk(id, "dbo.USP_ROOT"))
                .toList();

        List<RetrievedChunk> expanded = new DependencyExpander(dependencies, repository)
                .expand("portfolio", results, true);

        assertEquals(10, expanded.size());
        assertEquals(1L, expanded.get(0).chunkId());
        assertEquals(10L, expanded.get(9).chunkId());
    }

    private RetrievedChunk procedureChunk(long id, String title) {
        return new RetrievedChunk(id, "PROCEDURE", title, title, 1, 1, "body",
                null, null, 0, false, null);
    }
}
