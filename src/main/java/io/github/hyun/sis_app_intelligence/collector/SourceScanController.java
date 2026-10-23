package io.github.hyun.sis_app_intelligence.collector;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/source-files")
public class SourceScanController {

    private final SourceCollectorService sourceCollectorService;

    public SourceScanController(SourceCollectorService sourceCollectorService) {
        this.sourceCollectorService = sourceCollectorService;
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scan() {
        try {
            return ResponseEntity.ok(sourceCollectorService.scan());
        }
        catch (SourceRootNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "SOURCE_ROOT_NOT_CONFIGURED"));
        }
        catch (SourceRootNotFoundException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "SOURCE_ROOT_NOT_FOUND"));
        }
    }
}
