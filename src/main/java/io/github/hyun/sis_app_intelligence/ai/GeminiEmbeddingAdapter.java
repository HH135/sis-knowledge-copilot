package io.github.hyun.sis_app_intelligence.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

public class GeminiEmbeddingAdapter implements EmbeddingPort {

    private static final Logger LOGGER = Logger.getLogger(GeminiEmbeddingAdapter.class.getName());
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 250;
    private static final Pattern ERROR_STATUS_PATTERN =
            Pattern.compile("\"status\"\\s*:\\s*\"([A-Z0-9_]+)\"");
    private static final Pattern NUMERIC_VECTOR_PATTERN =
            Pattern.compile("\\[(?:\\s*-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\s*,?)+\\]");

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiEmbeddingAdapter(GeminiProperties properties) {
        this(properties, RestClient.builder().baseUrl(GEMINI_API_BASE_URL).build());
    }

    GeminiEmbeddingAdapter(GeminiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        return callBatch(texts, "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embedQuery(String text) {
        return callBatch(List.of(text), "RETRIEVAL_QUERY").getFirst();
    }

    @Override
    public String modelName() {
        return properties.getEmbeddingModel();
    }

    @Override
    public int dimensions() {
        return properties.getDimensions();
    }

    @Override
    public boolean sendsDataExternally() {
        return true;
    }

    private List<float[]> callBatch(List<String> texts, String taskType) {
        List<EmbedContentRequest> requests = texts.stream()
                .map(text -> new EmbedContentRequest(
                        "models/" + properties.getEmbeddingModel(),
                        new Content(List.of(new Part(text))),
                        taskType,
                        properties.getDimensions()))
                .toList();
        GeminiBatchRequest body = new GeminiBatchRequest(requests);
        GeminiBatchResponse response;
        try {
            response = executeWithRetry(body, texts);
        }
        catch (EmbeddingProviderException exception) {
            logFailure(exception, texts);
            throw exception;
        }
        if (response == null || response.embeddings() == null || response.embeddings().size() != texts.size()) {
            EmbeddingProviderException exception =
                    new EmbeddingProviderException("Gemini returned an unexpected embedding count.", "PARSE");
            logFailure(exception, texts);
            throw exception;
        }

        List<float[]> vectors = new ArrayList<>(response.embeddings().size());
        for (ContentEmbedding embedding : response.embeddings()) {
            if (embedding == null || embedding.values() == null) {
                EmbeddingProviderException exception =
                        new EmbeddingProviderException("Gemini response omitted embedding values.", "PARSE");
                logFailure(exception, texts);
                throw exception;
            }
            if (embedding.values().size() != properties.getDimensions()) {
                String lastError = "DIMENSION_MISMATCH expected=" + properties.getDimensions()
                        + " actual=" + embedding.values().size();
                EmbeddingProviderException exception = new EmbeddingProviderException(
                        "Gemini returned an unexpected embedding dimension.", lastError);
                logFailure(exception, texts);
                throw exception;
            }
            float[] vector = new float[embedding.values().size()];
            for (int index = 0; index < vector.length; index++) {
                Float value = embedding.values().get(index);
                if (value == null) {
                    EmbeddingProviderException exception =
                            new EmbeddingProviderException("Gemini response contained a null vector value.", "PARSE");
                    logFailure(exception, texts);
                    throw exception;
                }
                vector[index] = value;
            }
            vectors.add(vector);
        }
        return List.copyOf(vectors);
    }

    private GeminiBatchResponse executeWithRetry(GeminiBatchRequest body, List<String> texts) {
        for (int retry = 0; ; retry++) {
            try {
                return restClient.post()
                        .uri("/v1beta/models/{model}:batchEmbedContents", properties.getEmbeddingModel())
                        .header("x-goog-api-key", properties.getApiKey())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .exchange((request, response) -> {
                            int status = response.getStatusCode().value();
                            if (status < 200 || status >= 300) {
                                String errorBody = response.bodyTo(String.class);
                                throw new GeminiHttpException(status, extractErrorStatus(errorBody));
                            }
                            try {
                                return response.bodyTo(GeminiBatchResponse.class);
                            }
                            catch (RestClientException exception) {
                                throw new EmbeddingProviderException(
                                        "Gemini response JSON could not be parsed.", "PARSE", exception);
                            }
                        });
            }
            catch (GeminiHttpException exception) {
                if (!exception.isRetryable() || retry >= MAX_RETRIES) {
                    throw new EmbeddingProviderException(
                            "Gemini embedding request failed.",
                            "HTTP " + exception.status + " " + exception.errorStatus,
                            exception);
                }
                pauseBeforeRetry(retry);
            }
            catch (EmbeddingProviderException exception) {
                throw exception;
            }
            catch (RestClientException exception) {
                throw new EmbeddingProviderException(
                        "Gemini embedding request failed.", classifyTransportError(exception), exception);
            }
        }
    }

    private String extractErrorStatus(String errorBody) {
        Matcher matcher = ERROR_STATUS_PATTERN.matcher(errorBody == null ? "" : errorBody);
        return matcher.find() ? matcher.group(1) : "UNKNOWN";
    }

    private String classifyTransportError(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String className = current.getClass().getSimpleName();
            String message = current.getMessage();
            String lowerMessage = message == null ? "" : message.toLowerCase();
            if (current instanceof javax.net.ssl.SSLHandshakeException
                    || className.contains("CertPath")
                    || lowerMessage.contains("pkix")) {
                return "SSL_PKIX";
            }
            if (current instanceof java.net.UnknownHostException) {
                return "DNS";
            }
            if (current instanceof HttpMessageNotReadableException
                    || className.contains("JsonProcessingException")) {
                return "PARSE";
            }
        }
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String className = current.getClass().getSimpleName();
            String message = current.getMessage();
            if (current instanceof java.net.ConnectException
                    || className.toLowerCase().contains("proxy")
                    || (message != null && message.toLowerCase().contains("proxy"))) {
                return "CONNECT";
            }
        }
        return "CONNECT";
    }

    private void logFailure(EmbeddingProviderException exception, List<String> requestTexts) {
        StringBuilder chain = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (!chain.isEmpty()) {
                chain.append(" -> ");
            }
            String message = current.getMessage();
            chain.append(current.getClass().getName()).append(": ")
                    .append(redact(message == null ? "" : message, requestTexts));
        }
        LOGGER.warning("Gemini embedding request failed: lastError=" + exception.lastError()
                + ", causeChain=" + chain);
    }

    private String redact(String message, List<String> requestTexts) {
        String redacted = message;
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            redacted = redacted.replace(properties.getApiKey(), "[REDACTED]");
        }
        for (String text : requestTexts) {
            if (text != null && !text.isEmpty()) {
                redacted = redacted.replace(text, "[REDACTED]");
            }
        }
        return NUMERIC_VECTOR_PATTERN.matcher(redacted).replaceAll("[REDACTED_VECTOR]");
    }

    private void pauseBeforeRetry(int retry) {
        long delay = INITIAL_BACKOFF_MS << retry;
        try {
            Thread.sleep(Duration.ofMillis(delay));
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EmbeddingProviderException("Gemini retry was interrupted.", exception);
        }
    }

    private record EmbedContentRequest(String model, Content content, String taskType, int outputDimensionality) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GeminiBatchRequest(List<EmbedContentRequest> requests) {
    }

    private record GeminiBatchResponse(List<ContentEmbedding> embeddings) {
    }

    private record ContentEmbedding(List<Float> values) {
    }

    private static final class GeminiHttpException extends RuntimeException {
        private final int status;
        private final String errorStatus;

        private GeminiHttpException(int status, String errorStatus) {
            super("HTTP " + status + " " + errorStatus);
            this.status = status;
            this.errorStatus = errorStatus;
        }

        private boolean isRetryable() {
            return status == 429 || status >= 500;
        }
    }
}
