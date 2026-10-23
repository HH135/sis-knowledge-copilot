package io.github.hyun.sis_app_intelligence.ai;

public class EmbeddingProviderException extends RuntimeException {

    private final String lastError;

    public EmbeddingProviderException(String message) {
        this(message, "PROVIDER_ERROR", null);
    }

    public EmbeddingProviderException(String message, Throwable cause) {
        this(message, "PROVIDER_ERROR", cause);
    }

    public EmbeddingProviderException(String message, String lastError) {
        this(message, lastError, null);
    }

    public EmbeddingProviderException(String message, String lastError, Throwable cause) {
        super(message, cause);
        this.lastError = lastError;
    }

    public String lastError() {
        return lastError;
    }
}
