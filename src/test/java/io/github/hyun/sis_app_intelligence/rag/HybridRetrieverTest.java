package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridRetrieverTest {

    @Test
    void combinesBothListsWithReciprocalRankFusionAndPreservesSingleListResults() {
        FtsRetriever fts = mock(FtsRetriever.class);
        VectorRetriever vector = mock(VectorRetriever.class);
        when(fts.retrieve("portfolio", "question")).thenReturn(List.of(
                chunk(1, 1, null),
                chunk(2, 2, null)));
        when(vector.retrieve("portfolio", "question")).thenReturn(List.of(
                chunk(2, null, 1),
                chunk(3, null, 2)));

        List<RetrievedChunk> result = new HybridRetriever(fts, vector)
                .retrieve("portfolio", "question", 3);

        assertEquals(List.of(2L, 1L, 3L), result.stream().map(RetrievedChunk::chunkId).toList());
        assertEquals(1.0 / 62 + 1.0 / 61, result.get(0).rrfScore(), 0.000001);
        assertEquals(2, result.get(0).ftsRank());
        assertEquals(1, result.get(0).vectorRank());
        assertEquals(1.0 / 61, result.get(1).rrfScore(), 0.000001);
        assertEquals(1.0 / 62, result.get(2).rrfScore(), 0.000001);
    }

    private RetrievedChunk chunk(long id, Integer ftsRank, Integer vectorRank) {
        return new RetrievedChunk(id, "FILE", "title-" + id, "source-" + id, 1, 2,
                "content", ftsRank, vectorRank, 0, false, null);
    }
}
