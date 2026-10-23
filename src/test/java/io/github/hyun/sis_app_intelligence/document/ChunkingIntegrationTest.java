package io.github.hyun.sis_app_intelligence.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import io.github.hyun.sis_app_intelligence.collector.SourceCollectorService;
import io.github.hyun.sis_app_intelligence.metadata.MetadataSyncService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("portfolio")
@Transactional
class ChunkingIntegrationTest {

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private SourceCollectorService sourceCollectorService;

    @Autowired
    private ChunkingService chunkingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rebuildsFixtureChunksIdempotentlyAndCascadesEmbeddings() {
        metadataSyncService.sync();
        sourceCollectorService.scan();

        jdbcTemplate.update("""
                INSERT INTO document_chunk
                    (origin, source_kind, source_ref, title, start_line, end_line, content)
                VALUES ('other', 'FILE', 'other/example.java', 'other', 1, 1, 'other origin')
                """);

        ChunkResult first = chunkingService.rebuild();
        assertEquals(20, first.byKind().get("TABLE"));
        assertEquals(18, first.byKind().get("PROCEDURE"));
        assertEquals(14, first.byKind().get("MAPPER_STATEMENT"));
        assertEquals(0, first.mapperFallbacks());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunk WHERE origin = 'portfolio' "
                        + "AND title = 'com.acme.svc.appointment.AppointmentDao.register'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunk WHERE origin = 'portfolio' "
                        + "AND title = 'com.acme.svc.payment.PaymentDao.confirm'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunk WHERE origin = 'portfolio' "
                        + "AND title = 'com.acme.svc.common.CommonDao.retryInterfaceLog'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunk WHERE origin = 'other'", Integer.class));

        long embeddedChunkId = jdbcTemplate.queryForObject(
                "SELECT id FROM document_chunk WHERE origin = 'portfolio' LIMIT 1", Long.class);
        jdbcTemplate.update("""
                INSERT INTO chunk_embedding (chunk_id, model, dimensions, content_sha256, embedding)
                VALUES (?, 'mock-sha256-768', 768, ?, ?::vector)
                """, embeddedChunkId, "0".repeat(64), zeroVector(768));

        ChunkResult second = chunkingService.rebuild();

        assertEquals(first.total(), second.total());
        assertEquals(first.byKind(), second.byKind());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunk WHERE origin = 'other'", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM chunk_embedding WHERE chunk_id = ?", Integer.class, embeddedChunkId));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT search_vector IS NOT NULL FROM document_chunk WHERE origin = 'portfolio' LIMIT 1",
                Boolean.class));
    }

    private String zeroVector(int dimensions) {
        return "[1," + "0,".repeat(dimensions - 2) + "0]";
    }
}
