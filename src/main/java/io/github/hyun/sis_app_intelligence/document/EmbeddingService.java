package io.github.hyun.sis_app_intelligence.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import io.github.hyun.sis_app_intelligence.ai.EmbeddingPort;
import io.github.hyun.sis_app_intelligence.ai.EmbeddingProviderException;
import io.github.hyun.sis_app_intelligence.ai.EmbeddingVector;
import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

@Service
public class EmbeddingService {

    private static final Logger LOGGER = Logger.getLogger(EmbeddingService.class.getName());

    private final EmbeddingPort embeddingPort;
    private final ChunkEmbeddingRepository repository;
    private final GeminiProperties properties;

    public EmbeddingService(
            EmbeddingPort embeddingPort,
            ChunkEmbeddingRepository repository,
            GeminiProperties properties) {
        this.embeddingPort = embeddingPort;
        this.repository = repository;
        this.properties = properties;
    }

    public EmbeddingResult rebuild(boolean force) {
        long startedAt = System.nanoTime();
        List<DocumentChunk> chunks = repository.findPortfolioChunks(embeddingPort.modelName());
        List<PreparedChunk> pending = new ArrayList<>();
        int skippedUnchanged = 0;
        int truncated = 0;

        for (DocumentChunk chunk : chunks) {
            String fullText = chunk.title() + "\n\n" + chunk.content();
            String input = fullText.substring(0, Math.min(fullText.length(), properties.getMaxInputChars()));
            boolean wasTruncated = input.length() < fullText.length();
            if (!force && chunk.embeddingModel() != null
                    && sha256(input).equals(trimHash(chunk.contentSha256()))) {
                skippedUnchanged++;
                continue;
            }
            if (wasTruncated) {
                truncated++;
            }
            pending.add(new PreparedChunk(chunk, input, sha256(input)));
        }

        int embedded = 0;
        int failed = 0;
        String lastError = null;
        for (int start = 0; start < pending.size(); start += properties.getBatchSize()) {
            int end = Math.min(start + properties.getBatchSize(), pending.size());
            List<PreparedChunk> batch = pending.subList(start, end);
            try {
                List<float[]> vectors = embeddingPort.embedDocuments(
                        batch.stream().map(PreparedChunk::text).toList());
                if (vectors.size() != batch.size()) {
                    throw new EmbeddingProviderException("Embedding provider returned an unexpected vector count.");
                }
                List<ChunkEmbeddingRepository.EmbeddingRecord> records = new ArrayList<>(batch.size());
                for (int index = 0; index < batch.size(); index++) {
                    float[] normalized = EmbeddingVector.normalize(vectors.get(index), embeddingPort.dimensions());
                    PreparedChunk prepared = batch.get(index);
                    records.add(new ChunkEmbeddingRepository.EmbeddingRecord(
                            prepared.chunk().id(),
                            embeddingPort.modelName(),
                            embeddingPort.dimensions(),
                            prepared.sha256(),
                            normalized));
                }
                repository.saveBatch(records);
                embedded += records.size();
            }
            catch (RuntimeException exception) {
                failed = pending.size() - start;
                lastError = exception instanceof EmbeddingProviderException providerException
                        ? providerException.lastError()
                        : exception instanceof IllegalArgumentException
                                && exception.getMessage() != null
                                && exception.getMessage().toLowerCase().contains("dimension")
                                        ? "DIMENSION_MISMATCH"
                                        : "UNKNOWN";
                if (!(exception instanceof EmbeddingProviderException)) {
                    LOGGER.warning("Embedding rebuild failed: " + exception.getClass().getName()
                            + ": " + exception.getMessage());
                }
                break;
            }

            if (end < pending.size() && properties.getRequestIntervalMs() > 0) {
                try {
                    Thread.sleep(properties.getRequestIntervalMs());
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failed = pending.size() - end;
                    break;
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        LOGGER.info("Embedding rebuild completed: targetChunks=" + chunks.size()
                + ", embedded=" + embedded + ", skippedUnchanged=" + skippedUnchanged
                + ", truncated=" + truncated + ", failed=" + failed + ", elapsedMs=" + elapsedMs
                + ", lastError=" + lastError);
        return new EmbeddingResult(embeddingPort.modelName(), embeddingPort.dimensions(),
                chunks.size(), embedded, skippedUnchanged, truncated, failed, elapsedMs, lastError);
    }

    public List<SimilarChunk> findSimilar(String query, int limit) {
        float[] queryVector = EmbeddingVector.normalize(
                embeddingPort.embedQuery(query), embeddingPort.dimensions());
        return repository.findSimilar(queryVector, limit);
    }

    private String trimHash(String hash) {
        return hash == null ? null : hash.trim();
    }

    private String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private record PreparedChunk(DocumentChunk chunk, String text, String sha256) {
    }
}
