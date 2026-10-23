package io.github.hyun.sis_app_intelligence.document;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcedureChunkerTest {

    private final ProcedureChunker chunker = new ProcedureChunker();

    @Test
    void emitsOnlyNameAndDescriptionWhenDefinitionIsNotAccessible() {
        var chunks = chunker.chunk(new ChunkingInput(
                "internal", "PROCEDURE", "mssql:db.dbo.proc", "dbo.proc",
                "must not include", "procedure description", "NO_ACCESS"));

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().content().contains("dbo.proc"));
        assertTrue(chunks.getFirst().content().contains("procedure description"));
        assertTrue(!chunks.getFirst().content().contains("must not include"));
    }

    @Test
    void splitsLongProcedureDefinitionWithThirtyLineOverlap() {
        String body = IntStream.rangeClosed(1, 650).mapToObj(index -> "line-" + index)
                .collect(Collectors.joining("\n"));
        var chunks = chunker.chunk(new ChunkingInput(
                "portfolio", "PROCEDURE", "proc.sql", "dbo.proc", body, "description", "OK"));

        assertEquals(3, chunks.size());
        assertEquals(1, chunks.get(0).startLine());
        assertEquals(300, chunks.get(0).endLine());
        assertEquals(271, chunks.get(1).startLine());
        assertEquals(570, chunks.get(1).endLine());
        assertEquals(541, chunks.get(2).startLine());
        assertEquals(650, chunks.get(2).endLine());
    }
}
