package io.github.hyun.sis_app_intelligence.ai;

public class AnalysisProviderException extends RuntimeException {

    private final String lastError;

    public AnalysisProviderException(String message, String lastError) {
        super(message);
        this.lastError = lastError;
    }

    public AnalysisProviderException(String message, String lastError, Throwable cause) {
        super(message, cause);
        this.lastError = lastError;
    }

    public String lastError() {
        return lastError;
    }
}
