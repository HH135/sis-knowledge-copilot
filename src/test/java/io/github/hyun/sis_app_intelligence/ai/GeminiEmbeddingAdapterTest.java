package io.github.hyun.sis_app_intelligence.ai;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import io.github.hyun.sis_app_intelligence.config.GeminiProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiEmbeddingAdapterTest {

    private HttpServer server;
    private String requestBody;
    private String requestPath;
    private String apiKeyHeader;
    private String queryString;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestPath = exchange.getRequestURI().getPath();
            queryString = exchange.getRequestURI().getRawQuery();
            apiKeyHeader = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            int requestCount = requestBody.split("\"text\"").length - 1;
            byte[] response = responseJson(requestCount, 768).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsBatchRequestWithHeaderTaskTypeAndDimensions() {
        GeminiEmbeddingAdapter adapter = adapter();

        float[] vector = adapter.embedDocuments(java.util.List.of("document body", "second document")).getFirst();

        assertEquals(768, vector.length);
        assertEquals("/v1beta/models/gemini-embedding-001:batchEmbedContents", requestPath);
        assertEquals("test-key", apiKeyHeader);
        assertEquals(null, queryString);
        assertEquals(2, requestBody.split("\"model\":\"models/gemini-embedding-001\"").length - 1);
        assertTrue(requestBody.contains("\"taskType\":\"RETRIEVAL_DOCUMENT\""));
        assertTrue(requestBody.contains("\"outputDimensionality\":768"));
        assertTrue(requestBody.contains("\"text\":\"document body\""));
    }

    @Test
    void serializesSingleRequestInKnownWorkingGeminiFormat() {
        adapter().embedDocuments(java.util.List.of("test"));

        assertEquals(
                "{\"requests\":[{\"model\":\"models/gemini-embedding-001\","
                        + "\"content\":{\"parts\":[{\"text\":\"test\"}]},"
                        + "\"taskType\":\"RETRIEVAL_DOCUMENT\",\"outputDimensionality\":768}]}",
                requestBody);
    }

    @Test
    void parsesEmbeddingValuesArrayAs768Dimensions() {
        float[] vector = adapter().embedDocuments(java.util.List.of("test")).getFirst();

        assertEquals(768, vector.length);
        assertEquals(1.0f, vector[0]);
        assertEquals(1.0f, vector[767]);
    }

    @Test
    void usesRetrievalQueryTaskForQueries() {
        adapter().embedQuery("query");

        assertTrue(requestBody.contains("\"taskType\":\"RETRIEVAL_QUERY\""));
    }

    @Test
    void retries429And5xxWithBoundedRetries() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        server.removeContext("/");
        server.createContext("/", exchange -> {
            int call = calls.incrementAndGet();
            if (call < 3) {
                exchange.sendResponseHeaders(call == 1 ? 429 : 503, -1);
            }
            else {
                byte[] response = responseJson(1, 768).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });

        adapter().embedQuery("retry");

        assertEquals(3, calls.get());
    }

    @Test
    void errorsOnWrongVectorDimension() throws IOException {
        server.removeContext("/");
        server.createContext("/", exchange -> {
            byte[] response = responseJson(1, 767).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        EmbeddingProviderException exception = assertThrows(
                EmbeddingProviderException.class, () -> adapter().embedQuery("dimension mismatch"));

        assertEquals("DIMENSION_MISMATCH expected=768 actual=767", exception.lastError());
    }

    @Test
    void classifiesHttp400WithGoogleErrorStatus() throws IOException {
        server.removeContext("/");
        server.createContext("/", exchange -> {
            byte[] response = "{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        EmbeddingProviderException exception = assertThrows(
                EmbeddingProviderException.class, () -> adapter().embedQuery("invalid request"));

        assertEquals("HTTP 400 INVALID_ARGUMENT", exception.lastError());
    }

    @Test
    void classifiesMalformedResponseJsonAsParse() throws IOException {
        server.removeContext("/");
        server.createContext("/", exchange -> {
            byte[] response = "{\"embeddings\":[invalid".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        EmbeddingProviderException exception = assertThrows(
                EmbeddingProviderException.class, () -> adapter().embedQuery("parse error"));

        assertEquals("PARSE", exception.lastError());
    }

    private GeminiEmbeddingAdapter adapter() {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("test-key");
        properties.setRequestIntervalMs(0);
        RestClient client = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        return new GeminiEmbeddingAdapter(properties, client);
    }

    private String responseJson(int count, int dimensions) {
        String values = java.util.stream.IntStream.range(0, dimensions)
                .mapToObj(index -> "1")
                .collect(java.util.stream.Collectors.joining(","));
        String embedding = "{\"values\":[" + values + "]}";
        return "{\"embeddings\":[" + java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> embedding)
                .collect(java.util.stream.Collectors.joining(",")) + "]}";
    }
}
