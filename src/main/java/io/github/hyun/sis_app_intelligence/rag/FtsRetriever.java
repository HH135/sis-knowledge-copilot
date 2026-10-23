package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FtsRetriever {

    private static final int CANDIDATE_LIMIT = 20;
    private final RagRepository repository;

    public FtsRetriever(RagRepository repository) {
        this.repository = repository;
    }

    public List<RetrievedChunk> retrieve(String origin, String query) {
        return repository.searchFts(origin, query, CANDIDATE_LIMIT);
    }
}
