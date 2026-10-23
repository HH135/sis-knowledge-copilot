package io.github.hyun.sis_app_intelligence.rag;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@Valid @RequestBody RagAskRequest request) {
        return ResponseEntity.ok(ragService.ask(request));
    }
}
