package io.github.hyun.sis_app_intelligence.metadata;

public class MetadataSearchValidationException extends RuntimeException {

    private final String errorCode;

    public MetadataSearchValidationException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
