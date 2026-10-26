package io.github.hyun.sis_app_intelligence.rag;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.github.hyun.sis_app_intelligence.document.ChunkEmbeddingRepository;
import io.github.hyun.sis_app_intelligence.document.SimilarChunk;

@Repository
public class RagRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ChunkEmbeddingRepository embeddingRepository;

    public RagRepository(JdbcTemplate jdbcTemplate, ChunkEmbeddingRepository embeddingRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingRepository = embeddingRepository;
    }

    public List<RetrievedChunk> searchFts(String origin, String query, int limit) {
        List<RetrievedChunk> results = jdbcTemplate.query("""
                WITH search_query AS (
                    SELECT websearch_to_tsquery(
                        'simple', regexp_replace(?, '[[:space:]]+', ' OR ', 'g')) AS terms
                )
                SELECT chunk.id, chunk.source_kind, chunk.title, chunk.source_ref,
                       chunk.start_line, chunk.end_line, chunk.content,
                       ts_rank(
                           setweight(to_tsvector('simple', chunk.title), 'A')
                               || setweight(to_tsvector('simple', chunk.content), 'B'),
                           search_query.terms) AS rank_value
                FROM document_chunk chunk
                CROSS JOIN search_query
                WHERE chunk.origin = ? AND chunk.search_vector @@ search_query.terms
                ORDER BY rank_value DESC, chunk.id
                LIMIT ?
                """, (resultSet, rowNum) -> new RetrievedChunk(
                resultSet.getLong("id"),
                resultSet.getString("source_kind"),
                resultSet.getString("title"),
                resultSet.getString("source_ref"),
                (Integer) resultSet.getObject("start_line"),
                (Integer) resultSet.getObject("end_line"),
                resultSet.getString("content"),
                rowNum + 1,
                null,
                0,
                false,
                null), query, origin, limit);
        if (!results.isEmpty()) {
            return results;
        }

        String escapedQuery = escapeLike(query);
        String pattern = "%" + escapedQuery + "%";
        return jdbcTemplate.query("""
                SELECT id, source_kind, title, source_ref, start_line, end_line, content
                FROM document_chunk
                WHERE origin = ? AND (title ILIKE ? ESCAPE E'\\\\' OR content ILIKE ? ESCAPE E'\\\\')
                ORDER BY id
                LIMIT ?
                """, (resultSet, rowNum) -> new RetrievedChunk(
                resultSet.getLong("id"),
                resultSet.getString("source_kind"),
                resultSet.getString("title"),
                resultSet.getString("source_ref"),
                (Integer) resultSet.getObject("start_line"),
                (Integer) resultSet.getObject("end_line"),
                resultSet.getString("content"),
                rowNum + 1,
                null,
                0,
                false,
                null), origin, pattern, pattern, limit);
    }

    public List<RetrievedChunk> searchSimilar(String origin, float[] queryVector, int limit) {
        List<SimilarChunk> chunks = embeddingRepository.findSimilarForRetrieval(origin, queryVector, limit);
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> {
                    SimilarChunk chunk = chunks.get(index);
                    return new RetrievedChunk(
                            chunk.chunkId(),
                            chunk.sourceKind(),
                            chunk.title(),
                            chunk.sourceRef(),
                            chunk.startLine(),
                            chunk.endLine(),
                            chunk.content(),
                            null,
                            index + 1,
                            0,
                            false,
                            null);
                })
                .toList();
    }

    public List<RetrievedChunk> findProcedureChunks(String origin, String procedureName) {
        return jdbcTemplate.query("""
                SELECT id, source_kind, title, source_ref, start_line, end_line, content
                FROM document_chunk
                WHERE origin = ? AND source_kind = 'PROCEDURE' AND lower(title) = lower(?)
                ORDER BY start_line
                """, (resultSet, rowNum) -> new RetrievedChunk(
                resultSet.getLong("id"),
                resultSet.getString("source_kind"),
                resultSet.getString("title"),
                resultSet.getString("source_ref"),
                (Integer) resultSet.getObject("start_line"),
                (Integer) resultSet.getObject("end_line"),
                resultSet.getString("content"),
                null,
                null,
                0,
                true,
                null), origin, procedureName);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

}
