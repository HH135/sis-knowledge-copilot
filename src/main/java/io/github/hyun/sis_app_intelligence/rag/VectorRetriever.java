package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.hyun.sis_app_intelligence.ai.EmbeddingPort;
import io.github.hyun.sis_app_intelligence.ai.EmbeddingVector;

@Component
public class VectorRetriever {

    private static final int CANDIDATE_LIMIT = 20;
    private final EmbeddingPort embeddingPort;
    private final RagRepository repository;

    public VectorRetriever(EmbeddingPort embeddingPort, RagRepository repository) {
        this.embeddingPort = embeddingPort;
        this.repository = repository;
    }

    public List<RetrievedChunk> retrieve(String origin, String query) {
        float[] vector = EmbeddingVector.normalize(embeddingPort.embedQuery(query), embeddingPort.dimensions());
        return repository.searchSimilar(origin, vector, CANDIDATE_LIMIT);
    }
}
