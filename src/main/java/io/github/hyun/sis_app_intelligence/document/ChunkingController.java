package io.github.hyun.sis_app_intelligence.document;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chunks")
public class ChunkingController {

    private final ChunkingService chunkingService;

    public ChunkingController(ChunkingService chunkingService) {
        this.chunkingService = chunkingService;
    }

    @PostMapping("/rebuild")
    public ChunkResult rebuild() {
        return chunkingService.rebuild();
    }
}
