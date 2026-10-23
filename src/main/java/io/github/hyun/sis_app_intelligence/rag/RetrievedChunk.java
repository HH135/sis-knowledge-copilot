package io.github.hyun.sis_app_intelligence.rag;

public record RetrievedChunk(
        long chunkId,
        String sourceKind,
        String title,
        String sourceRef,
        Integer startLine,
        Integer endLine,
        String content,
        Integer ftsRank,
        Integer vectorRank,
        double rrfScore,
        boolean expanded,
        String expandedFrom
) {

    public RetrievedChunk asExpanded(String sourceProcedure) {
        return new RetrievedChunk(chunkId, sourceKind, title, sourceRef, startLine, endLine,
                content, null, null, 0, true, sourceProcedure);
    }
}
