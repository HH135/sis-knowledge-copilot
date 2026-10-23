package io.github.hyun.sis_app_intelligence.document;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.hyun.sis_app_intelligence.ai.EmbeddingPort;
import io.github.hyun.sis_app_intelligence.ai.EmbeddingProviderException;
import io.github.hyun.sis_app_intelligence.ai.MockEmbeddingAdapter;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingServiceTest {

    @Test
    void reportsUnchangedTruncatedAndEmbeddedChunks() {
        MockEmbeddingAdapter adapter = new MockEmbeddingAdapter();
        ChunkEmbeddingRepository repository = mock(ChunkEmbeddingRepository.class);
        GeminiProperties properties = new GeminiProperties();
        properties.setMaxInputChars(10);
        properties.setRequestIntervalMs(0);
        DocumentChunk changed = chunk(1, "First title", "content that is long", null, null);
        String unchangedText = "Same title\n\nsame body";
        String unchangedHash = TestHash.sha256(unchangedText.substring(0, 10));
        DocumentChunk unchanged = chunk(2, "Same title", "same body", unchangedHash, adapter.modelName());
        when(repository.findPortfolioChunks(adapter.modelName())).thenReturn(List.of(changed, unchanged));

        EmbeddingResult result = new EmbeddingService(adapter, repository, properties).rebuild(false);

        assertEquals(2, result.targetChunks());
        assertEquals(1, result.embedded());
        assertEquals(1, result.skippedUnchanged());
        assertEquals(1, result.truncated());
        assertEquals(0, result.failed());
        org.mockito.Mockito.verify(repository).saveBatch(org.mockito.ArgumentMatchers.argThat(
                batch -> batch.size() == 1 && batch.getFirst().chunkId() == 1));
    }

    @Test
    void preservesSavedBatchAndCountsRemainingFailures() {
        EmbeddingPort adapter = new EmbeddingPort() {
            private final MockEmbeddingAdapter mock = new MockEmbeddingAdapter();
            private int calls;

            @Override
            public List<float[]> embedDocuments(List<String> texts) {
                if (++calls > 1) {
                    throw new EmbeddingProviderException("provider failed");
                }
                return mock.embedDocuments(texts);
            }

            @Override
            public float[] embedQuery(String text) {
                return mock.embedQuery(text);
            }

            @Override
            public String modelName() {
                return mock.modelName();
            }

            @Override
            public int dimensions() {
                return mock.dimensions();
            }

            @Override
            public boolean sendsDataExternally() {
                return false;
            }
        };
        ChunkEmbeddingRepository repository = mock(ChunkEmbeddingRepository.class);
        GeminiProperties properties = new GeminiProperties();
        properties.setBatchSize(1);
        properties.setRequestIntervalMs(0);
        when(repository.findPortfolioChunks(adapter.modelName())).thenReturn(List.of(
                chunk(1, "one", "one", null, null),
                chunk(2, "two", "two", null, null),
                chunk(3, "three", "three", null, null)));

        EmbeddingResult result = new EmbeddingService(adapter, repository, properties).rebuild(false);

        assertEquals(1, result.embedded());
        assertEquals(2, result.failed());
        assertEquals("PROVIDER_ERROR", result.lastError());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(1)).saveBatch(
                org.mockito.ArgumentMatchers.anyList());
    }

    private DocumentChunk chunk(long id, String title, String content, String hash, String model) {
        return new DocumentChunk(id, "java", "portfolio/" + id, title, 1, 2, content, hash, model);
    }

    private static final class TestHash {
        private static String sha256(String text) {
            try {
                byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return java.util.HexFormat.of().formatHex(bytes);
            }
            catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
