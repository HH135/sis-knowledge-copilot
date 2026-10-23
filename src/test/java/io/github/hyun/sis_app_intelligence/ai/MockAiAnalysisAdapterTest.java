package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockAiAnalysisAdapterTest {

    @Test
    void returnsDeterministicAnswerAndMapsCitations() {
        MockAiAnalysisAdapter adapter = new MockAiAnalysisAdapter();
        AnalysisRequest request = new AnalysisRequest("Why is the order pending?", List.of(
                new ContextChunk("source", "src/Order.java", "OrderService", 10, 20, "source text"),
                new ContextChunk("procedure", "dbo.USP_SAMPLE", "Order procedure", 1, 5, "procedure text")));

        AnalysisResult first = adapter.analyze(request);
        AnalysisResult second = adapter.analyze(request);

        assertEquals(first, second);
        assertEquals("[MOCK] Why is the order pending? + OrderService, Order procedure", first.answer());
        assertEquals(List.of(
                new Citation("source", "src/Order.java", "OrderService", 10, 20),
                new Citation("procedure", "dbo.USP_SAMPLE", "Order procedure", 1, 5)), first.citations());
        assertEquals("mock", first.provider());
        assertFalse(adapter.sendsDataExternally());
    }
}
