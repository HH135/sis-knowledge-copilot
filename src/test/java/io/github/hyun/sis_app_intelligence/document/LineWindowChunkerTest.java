package io.github.hyun.sis_app_intelligence.document;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineWindowChunkerTest {

    private final LineWindowChunker chunker = new LineWindowChunker();

    @Test
    void keepsFiftyLinesInOneChunk() {
        var chunks = chunker.chunk(input(lines(50)));

        assertEquals(1, chunks.size());
        assertEquals(1, chunks.getFirst().startLine());
        assertEquals(50, chunks.getFirst().endLine());
    }

    @Test
    void splitsTwoHundredFiftyLinesWithTwentyLineOverlap() {
        var chunks = chunker.chunk(input(lines(250)));

        assertEquals(3, chunks.size());
        assertEquals(1, chunks.get(0).startLine());
        assertEquals(120, chunks.get(0).endLine());
        assertEquals(101, chunks.get(1).startLine());
        assertEquals(220, chunks.get(1).endLine());
        assertEquals(201, chunks.get(2).startLine());
        assertEquals(250, chunks.get(2).endLine());
        assertTrue(chunks.get(0).content().endsWith("line-120"));
        assertTrue(chunks.get(1).content().startsWith("line-101"));
    }

    private ChunkingInput input(String content) {
        return new ChunkingInput("portfolio", "FILE", "sample.java", "sample.java", content, null, null);
    }

    private String lines(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(index -> "line-" + index).collect(Collectors.joining("\n"));
    }
}
