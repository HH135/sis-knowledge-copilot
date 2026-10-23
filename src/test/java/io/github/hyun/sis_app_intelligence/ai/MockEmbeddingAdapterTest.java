package io.github.hyun.sis_app_intelligence.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockEmbeddingAdapterTest {

    @Test
    void createsDeterministicNormalized768DimensionalVectors() {
        MockEmbeddingAdapter adapter = new MockEmbeddingAdapter();
        float[] first = adapter.embedQuery("Acme repair order");
        float[] second = adapter.embedQuery("Acme repair order");
        double norm = 0;
        for (float value : first) {
            norm += (double) value * value;
        }

        assertEquals(768, first.length);
        assertArrayEquals(first, second);
        assertEquals(1.0, Math.sqrt(norm), 0.00001);
        assertEquals("mock-sha256-768", adapter.modelName());
        assertEquals(768, adapter.dimensions());
        assertFalse(adapter.sendsDataExternally());
    }
}
