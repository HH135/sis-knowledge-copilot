package io.github.hyun.sis_app_intelligence.rag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.hyun.sis_app_intelligence.dependency.CallTreeNode;
import io.github.hyun.sis_app_intelligence.dependency.DependencyService;
import io.github.hyun.sis_app_intelligence.dependency.ProcedureNotFoundException;

@Component
public class DependencyExpander {

    private static final int EXPANSION_DEPTH = 3;
    private static final int MAX_CONTEXTS = 10;

    private final DependencyService dependencyService;
    private final RagRepository repository;

    public DependencyExpander(DependencyService dependencyService, RagRepository repository) {
        this.dependencyService = dependencyService;
        this.repository = repository;
    }

    public List<RetrievedChunk> expand(String origin, List<RetrievedChunk> searchResults, boolean expand) {
        Map<Long, RetrievedChunk> contexts = new LinkedHashMap<>();
        for (RetrievedChunk chunk : searchResults) {
            if (contexts.size() == MAX_CONTEXTS) {
                break;
            }
            contexts.putIfAbsent(chunk.chunkId(), chunk);
        }
        if (!expand || contexts.size() == MAX_CONTEXTS) {
            return List.copyOf(contexts.values());
        }

        for (RetrievedChunk root : List.copyOf(contexts.values())) {
            if (!"PROCEDURE".equalsIgnoreCase(root.sourceKind()) || contexts.size() == MAX_CONTEXTS) {
                continue;
            }
            try {
                CallTreeNode tree = dependencyService.callTree(root.title(), EXPANSION_DEPTH);
                addResolvedChildren(origin, tree.children(), root.title(), contexts);
            }
            catch (ProcedureNotFoundException ignored) {
                // A procedure chunk without metadata cannot be expanded.
            }
        }
        return List.copyOf(contexts.values());
    }

    private void addResolvedChildren(
            String origin,
            List<CallTreeNode> nodes,
            String expandedFrom,
            Map<Long, RetrievedChunk> contexts) {
        for (CallTreeNode node : nodes) {
            if (contexts.size() >= MAX_CONTEXTS) {
                return;
            }
            if (!"RESOLVED".equals(node.callType()) || node.cycle()) {
                continue;
            }
            for (RetrievedChunk chunk : repository.findProcedureChunks(origin, node.procedure())) {
                if (contexts.size() >= MAX_CONTEXTS) {
                    return;
                }
                contexts.putIfAbsent(chunk.chunkId(), chunk.asExpanded(expandedFrom));
            }
            addResolvedChildren(origin, node.children(), expandedFrom, contexts);
        }
    }
}
