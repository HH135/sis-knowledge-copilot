package io.github.hyun.sis_app_intelligence.ai;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

public class GeminiAnalysisAdapter implements AiAnalysisPort {

    private static final Logger LOGGER = Logger.getLogger(GeminiAnalysisAdapter.class.getName());
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String SYSTEM_INSTRUCTION = """
            주어진 컨텍스트만 근거로 한국어로 답한다. 문장마다 근거 컨텍스트 번호를 [n] 형식으로 붙인다.
            근거가 없으면 '제공된 자료로는 확인할 수 없습니다'라고 답한다.
            컨텍스트에 없는 테이블/프로시저 이름을 만들지 마라.
            """;
    private static final Pattern ERROR_STATUS_PATTERN =
            Pattern.compile("\"status\"\\s*:\\s*\"([A-Z0-9_]+)\"");

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiAnalysisAdapter(GeminiProperties properties) {
        this(properties, RestClient.builder().baseUrl(BASE_URL).build());
    }

    GeminiAnalysisAdapter(GeminiProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        String userPrompt = formatPrompt(request.contexts());
        try {
            GeminiResponse response = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    response = send(request.question(), userPrompt);
                    break;
                }
                catch (GeminiAnalysisHttpException exception) {
                    if (exception.status != 429 || attempt > 0) {
                        throw exception;
                    }
                }
            }

            String answer = response == null || response.candidates() == null
                    ? null
                    : response.candidates().stream()
                            .filter(candidate -> candidate.content() != null
                                    && candidate.content().parts() != null)
                            .flatMap(candidate -> candidate.content().parts().stream())
                            .map(Part::text)
                            .filter(text -> text != null)
                            .findFirst()
                            .orElse(null);
            if (answer == null) {
                throw new AnalysisProviderException("Gemini response did not contain answer text.", "PARSE");
            }
            return new AnalysisResult(answer, List.of(), providerName());
        }
        catch (GeminiAnalysisHttpException exception) {
            String lastError = "HTTP " + exception.status + " " + exception.errorStatus;
            logFailure(exception, lastError);
            throw new AnalysisProviderException("Gemini analysis request failed.", lastError, exception);
        }
        catch (AnalysisProviderException exception) {
            logFailure(exception, exception.lastError());
            throw exception;
        }
        catch (RestClientException exception) {
            String lastError = classify(exception);
            logFailure(exception, lastError);
            throw new AnalysisProviderException("Gemini analysis request failed.", lastError, exception);
        }
    }

    private GeminiResponse send(String question, String userPrompt) {
        return restClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.getChatModel())
                .header("x-goog-api-key", properties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(new GeminiRequest(
                        new SystemInstruction(List.of(new Part(SYSTEM_INSTRUCTION))),
                        List.of(new Content("user", List.of(new Part(
                                "Question:\n" + question + "\n\n" + userPrompt)))),
                        new GenerationConfig(0.2)))
                .exchange((httpRequest, httpResponse) -> {
                    int status = httpResponse.getStatusCode().value();
                    if (status < 200 || status >= 300) {
                        String body = httpResponse.bodyTo(String.class);
                        throw new GeminiAnalysisHttpException(status, errorStatus(body));
                    }
                    try {
                        return httpResponse.bodyTo(GeminiResponse.class);
                    }
                    catch (RestClientException exception) {
                        throw new AnalysisProviderException(
                                "Gemini response could not be parsed.", "PARSE", exception);
                    }
                });
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public boolean sendsDataExternally() {
        return true;
    }

    private String formatPrompt(List<ContextChunk> contexts) {
        StringBuilder prompt = new StringBuilder("Contexts:\n");
        for (int index = 0; index < contexts.size(); index++) {
            ContextChunk context = contexts.get(index);
            String content = context.content() == null ? "" : context.content();
            if (content.length() > 4000) {
                content = content.substring(0, 4000);
            }
            prompt.append('[').append(index + 1).append("] title=").append(context.title())
                    .append(", sourceRef=").append(context.sourceRef())
                    .append(", lines=").append(context.startLine()).append('-').append(context.endLine())
                    .append('\n').append(content).append("\n\n");
        }
        return prompt.toString();
    }

    private String errorStatus(String body) {
        Matcher matcher = ERROR_STATUS_PATTERN.matcher(body == null ? "" : body);
        return matcher.find() ? matcher.group(1) : "UNKNOWN";
    }

    private String classify(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String className = current.getClass().getSimpleName();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (current instanceof SSLHandshakeException || className.contains("CertPath") || message.contains("pkix")) {
                return "SSL_PKIX";
            }
            if (current instanceof UnknownHostException) {
                return "DNS";
            }
            if (current instanceof HttpMessageNotReadableException || className.contains("JsonProcessingException")) {
                return "PARSE";
            }
        }
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String className = current.getClass().getSimpleName();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (current instanceof ConnectException || className.toLowerCase().contains("proxy")
                    || message.contains("proxy")) {
                return "CONNECT";
            }
        }
        return "CONNECT";
    }

    private void logFailure(Throwable throwable, String lastError) {
        StringBuilder chain = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (!chain.isEmpty()) {
                chain.append(" -> ");
            }
            chain.append(current.getClass().getName());
        }
        LOGGER.warning("Gemini analysis failed: lastError=" + lastError + ", causeTypes=" + chain);
    }

    private record GeminiRequest(
            SystemInstruction systemInstruction,
            List<Content> contents,
            GenerationConfig generationConfig) {
    }

    private record SystemInstruction(List<Part> parts) {
    }

    private record Content(String role, List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerationConfig(double temperature) {
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private static final class GeminiAnalysisHttpException extends RuntimeException {
        private final int status;
        private final String errorStatus;

        private GeminiAnalysisHttpException(int status, String errorStatus) {
            super("HTTP " + status + " " + errorStatus);
            this.status = status;
            this.errorStatus = errorStatus;
        }
    }
}
