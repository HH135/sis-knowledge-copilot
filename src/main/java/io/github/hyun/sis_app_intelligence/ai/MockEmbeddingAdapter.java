package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingAdapter implements EmbeddingPort {

    private static final int DIMENSIONS = 768;

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        return texts.stream().map(this::vectorFor).toList();
    }

    @Override
    public float[] embedQuery(String text) {
        return vectorFor(text);
    }

    @Override
    public String modelName() {
        return "mock-token-hash-768";
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public boolean sendsDataExternally() {
        return false;
    }

    private float[] vectorFor(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (token.isEmpty()) {
                continue;
            }
            int hash = token.hashCode();
            int index = Math.floorMod(hash, DIMENSIONS);
            vector[index] += (Integer.bitCount(hash) & 1) == 0 ? 1 : -1;
        }
        return EmbeddingVector.normalize(vector, DIMENSIONS);
    }
}
