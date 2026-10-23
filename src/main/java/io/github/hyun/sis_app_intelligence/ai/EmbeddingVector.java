package io.github.hyun.sis_app_intelligence.ai;

public final class EmbeddingVector {

    private EmbeddingVector() {
    }

    public static float[] normalize(float[] vector, int expectedDimensions) {
        if (vector == null || vector.length != expectedDimensions) {
            throw new IllegalArgumentException("Embedding dimension does not match configured dimensions.");
        }
        double squaredNorm = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Embedding contains a non-finite value.");
            }
            squaredNorm += (double) value * value;
        }
        if (squaredNorm == 0) {
            throw new IllegalArgumentException("Embedding vector cannot be zero.");
        }
        double norm = Math.sqrt(squaredNorm);
        float[] normalized = new float[vector.length];
        for (int index = 0; index < vector.length; index++) {
            normalized[index] = (float) (vector[index] / norm);
        }
        return normalized;
    }
}
