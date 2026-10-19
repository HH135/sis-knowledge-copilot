package io.github.hyun.sis_app_intelligence.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableChunkerTest {

    @Test
    void includesTableDescriptionAndColumnLines() {
        var chunks = new TableChunker().chunk(new ChunkingInput(
                "portfolio", "TABLE", "dbo.TB_ORDER", "dbo.TB_ORDER",
                "ORDER_NO varchar(20) NOT NULL order number\nCUST_NO varchar(20) NULL customer",
                "Repair order", null));

        assertEquals(1, chunks.size());
        assertEquals(1, chunks.getFirst().startLine());
        assertTrue(chunks.getFirst().content().contains("dbo.TB_ORDER / Repair order"));
        assertTrue(chunks.getFirst().content().contains("ORDER_NO varchar(20) NOT NULL order number"));
    }
}
