package io.github.hyun.sis_app_intelligence.metadata;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata")
public class MetadataSearchController {

    private final MetadataSearchService metadataSearchService;

    public MetadataSearchController(MetadataSearchService metadataSearchService) {
        this.metadataSearchService = metadataSearchService;
    }

    @GetMapping("/columns")
    public ResponseEntity<?> searchColumns(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String table,
            @RequestParam(required = false) Integer limit) {
        try {
            return ResponseEntity.ok(metadataSearchService.searchColumns(q, table, limit));
        }
        catch (MetadataSearchValidationException exception) {
            return badRequest(exception);
        }
    }

    @GetMapping("/procedures")
    public ResponseEntity<?> searchProcedures(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean body,
            @RequestParam(required = false) Integer limit) {
        try {
            return ResponseEntity.ok(metadataSearchService.searchProcedures(q, body, limit));
        }
        catch (MetadataSearchValidationException exception) {
            return badRequest(exception);
        }
    }

    private ResponseEntity<Map<String, String>> badRequest(MetadataSearchValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.errorCode()));
    }
}
