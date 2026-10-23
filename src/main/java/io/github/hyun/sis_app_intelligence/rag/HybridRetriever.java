package io.github.hyun.sis_app_intelligence.rag;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class HybridRetriever {

    private static final int RRF_CONSTANT = 60;
    private final FtsRetriever ftsRetriever;
    private final VectorRetriever vectorRetriever;

    public HybridRetriever(FtsRetriever ftsRetriever, VectorRetriever vectorRetriever) {
        this.ftsRetriever = ftsRetriever;
        this.vectorRetriever = vectorRetriever;
    }

    public List<RetrievedChunk> retrieve(String origin, String query, int limit) {
        List<RetrievedChunk> fts = ftsRetriever.retrieve(origin, query);
        List<RetrievedChunk> vector = vectorRetriever.retrieve(origin, query);
        Map<Long, Combined> combined = new LinkedHashMap<>();
        for (RetrievedChunk chunk : fts) {
            combined.put(chunk.chunkId(), new Combined(chunk, chunk.ftsRank(), null,
                    reciprocalRank(chunk.ftsRank())));
        }
        for (RetrievedChunk chunk : vector) {
            Combined prior = combined.get(chunk.chunkId());
            if (prior == null) {
                combined.put(chunk.chunkId(), new Combined(chunk, null, chunk.vectorRank(),
                        reciprocalRank(chunk.vectorRank())));
            }
            else {
                combined.put(chunk.chunkId(), new Combined(prior.chunk, prior.ftsRank, chunk.vectorRank(),
                        prior.score + reciprocalRank(chunk.vectorRank())));
            }
        }
        return combined.values().stream()
                .sorted(Comparator.comparingDouble(Combined::score).reversed()
                        .thenComparingLong(value -> value.chunk.chunkId()))
                .limit(limit)
                .map(value -> new RetrievedChunk(
                        value.chunk.chunkId(),
                        value.chunk.sourceKind(),
                        value.chunk.title(),
                        value.chunk.sourceRef(),
                        value.chunk.startLine(),
                        value.chunk.endLine(),
                        value.chunk.content(),
                        value.ftsRank,
                        value.vectorRank,
                        value.score,
                        false,
                        null))
                .toList();
    }

    private double reciprocalRank(Integer rank) {
        return rank == null ? 0 : 1.0 / (RRF_CONSTANT + rank);
    }

    private record Combined(RetrievedChunk chunk, Integer ftsRank, Integer vectorRank, double score) {
    }
}
