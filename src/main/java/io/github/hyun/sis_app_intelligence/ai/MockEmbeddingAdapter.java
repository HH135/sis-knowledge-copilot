package io.github.hyun.sis_app_intelligence.ai;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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
        return "mock-sha256-768";
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
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int offset = 0;
            int counter = 0;
            while (offset < DIMENSIONS) {
                digest.update(input);
                byte[] block = digest.digest(ByteBuffer.allocate(Integer.BYTES).putInt(counter++).array());
                for (int index = 0; index + Float.BYTES <= block.length && offset < DIMENSIONS; index += Float.BYTES) {
                    int bits = ByteBuffer.wrap(block, index, Float.BYTES).getInt();
                    vector[offset++] = (bits / (float) Integer.MAX_VALUE);
                }
            }
            return EmbeddingVector.normalize(vector, DIMENSIONS);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
