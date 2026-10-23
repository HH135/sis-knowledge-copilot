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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiAnalysisAdapterTest {

    private static final String PATH = "/v1beta/models/test-chat-model:generateContent";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String requestBody;
    private String apiKeyHeader;
    private int status;
    private String responseBody;
    private AtomicInteger requestCount;

    @BeforeEach
    void startServer() throws IOException {
        requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(PATH, exchange -> {
            requestCount.incrementAndGet();
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            apiKeyHeader = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsNumberedContextAndParsesCandidateText() throws Exception {
        status = 200;
        responseBody = """
                {"candidates":[{"content":{"parts":[{"text":"answer [1]"}]}}]}
                """;

        AnalysisResult result = adapter().analyze(new AnalysisRequest(
                "question", java.util.List.of(new ContextChunk(
                        "PROCEDURE", "dbo.USP_SAMPLE", "dbo.USP_SAMPLE", 2, 5, "context body"))));

        JsonNode request = objectMapper.readTree(requestBody);
        assertEquals("secret-test-key", apiKeyHeader);
        assertEquals("answer [1]", result.answer());
        assertEquals(0.2, request.path("generationConfig").path("temperature").asDouble());
        assertTrue(request.path("systemInstruction").path("parts").get(0).path("text").asText()
                .contains("문장마다 근거 컨텍스트 번호를 [n]"));
        assertTrue(request.path("contents").get(0).path("parts").get(0).path("text").asText()
                .contains("[1] title=dbo.USP_SAMPLE"));
        assertTrue(request.path("contents").get(0).path("parts").get(0).path("text").asText()
                .contains("context body"));
    }

    @Test
    void retriesRateLimitOnceAndReturnsClassifiedHttpErrorAfterSecondFailure() {
        status = 429;
        responseBody = """
                {"error":{"status":"RESOURCE_EXHAUSTED"}}
                """;

        try {
            adapter().analyze(new AnalysisRequest("question", java.util.List.of()));
        }
        catch (AnalysisProviderException exception) {
            assertEquals("HTTP 429 RESOURCE_EXHAUSTED", exception.lastError());
            assertEquals(2, requestCount.get());
            return;
        }
        throw new AssertionError("Expected a provider exception.");
    }

    private GeminiAnalysisAdapter adapter() {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("secret-test-key");
        properties.setChatModel("test-chat-model");
        RestClient restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        return new GeminiAnalysisAdapter(properties, restClient);
    }
}
