package io.github.hyun.sis_app_intelligence.document;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ChunkEmbeddingRepository {

    private static final RowMapper<DocumentChunk> CHUNK_ROW_MAPPER = (resultSet, rowNum) ->
            new DocumentChunk(
                    resultSet.getLong("id"),
                    resultSet.getString("source_kind"),
                    resultSet.getString("source_ref"),
                    resultSet.getString("title"),
                    (Integer) resultSet.getObject("start_line"),
                    (Integer) resultSet.getObject("end_line"),
                    resultSet.getString("content"),
                    resultSet.getString("content_sha256"),
                    resultSet.getString("embedding_model"));

    private static final RowMapper<SimilarChunk> SIMILAR_CHUNK_ROW_MAPPER = (resultSet, rowNum) ->
            new SimilarChunk(
                    resultSet.getLong("chunk_id"),
                    resultSet.getString("source_kind"),
                    resultSet.getString("title"),
                    resultSet.getString("source_ref"),
                    (Integer) resultSet.getObject("start_line"),
                    (Integer) resultSet.getObject("end_line"),
                    resultSet.getDouble("score"),
                    resultSet.getString("content"));

    private final JdbcTemplate jdbcTemplate;

    public ChunkEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DocumentChunk> findPortfolioChunks(String model) {
        return jdbcTemplate.query("""
                SELECT c.id, c.source_kind, c.source_ref, c.title, c.start_line, c.end_line,
                       c.content, e.content_sha256,
                       CASE WHEN e.model = ? THEN e.model ELSE NULL END AS embedding_model
                FROM document_chunk c
                LEFT JOIN chunk_embedding e ON e.chunk_id = c.id
                WHERE c.origin = 'portfolio'
                ORDER BY c.id
                """, CHUNK_ROW_MAPPER, model);
    }

    @Transactional
    public void saveBatch(List<EmbeddingRecord> records) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO chunk_embedding
                    (chunk_id, model, dimensions, content_sha256, embedding, created_at)
                VALUES (?, ?, ?, ?, ?::vector, now())
                ON CONFLICT (chunk_id) DO UPDATE
                SET model = EXCLUDED.model,
                    dimensions = EXCLUDED.dimensions,
                    content_sha256 = EXCLUDED.content_sha256,
                    embedding = EXCLUDED.embedding,
                    created_at = now()
                """, records, records.size(), (statement, record) -> {
                    statement.setLong(1, record.chunkId());
                    statement.setString(2, record.model());
                    statement.setInt(3, record.dimensions());
                    statement.setString(4, record.contentSha256());
                    statement.setString(5, vectorLiteral(record.embedding()));
                });
    }

    public List<SimilarChunk> findSimilar(float[] queryVector, int limit) {
        return jdbcTemplate.query("""
                SELECT c.id AS chunk_id, c.source_kind, c.title, c.source_ref,
                       c.start_line, c.end_line,
                       1 - (e.embedding <=> ?::vector) AS score,
                       left(c.content, 200) AS content
                FROM chunk_embedding e
                JOIN document_chunk c ON c.id = e.chunk_id
                WHERE c.origin = 'portfolio'
                  AND e.dimensions = ?
                ORDER BY e.embedding <=> ?::vector
                LIMIT ?
                """, SIMILAR_CHUNK_ROW_MAPPER,
                vectorLiteral(queryVector), queryVector.length, vectorLiteral(queryVector), limit);
    }

    private String vectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(Float.toString(vector[index]));
        }
        return literal.append(']').toString();
    }

    public record EmbeddingRecord(long chunkId, String model, int dimensions, String contentSha256, float[] embedding) {
    }
}
