package io.github.hyun.sis_app_intelligence.document;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.hyun.sis_app_intelligence.ai.EmbeddingProviderException;

@RestController
@Profile("portfolio")
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/rebuild")
    public ResponseEntity<?> rebuild(@RequestParam(defaultValue = "false") boolean force) {
        try {
            return ResponseEntity.ok(embeddingService.rebuild(force));
        }
        catch (EmbeddingProviderException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "EMBEDDING_PROVIDER_UNAVAILABLE",
                            "lastError", exception.lastError()));
        }
    }

    @GetMapping("/similar")
    public ResponseEntity<?> similar(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int k) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "QUERY_REQUIRED"));
        }
        if (q.length() > 6000) {
            return ResponseEntity.badRequest().body(Map.of("error", "QUERY_TOO_LONG"));
        }
        if (k < 1 || k > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_LIMIT"));
        }
        try {
            List<SimilarChunk> items = embeddingService.findSimilar(q, k);
            return ResponseEntity.ok(Map.of("query", q, "count", items.size(), "items", items));
        }
        catch (EmbeddingProviderException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "EMBEDDING_PROVIDER_UNAVAILABLE",
                            "lastError", exception.lastError()));
        }
    }
}
