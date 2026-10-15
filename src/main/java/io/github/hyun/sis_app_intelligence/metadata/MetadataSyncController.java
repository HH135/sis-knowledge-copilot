package io.github.hyun.sis_app_intelligence.metadata;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata")
public class MetadataSyncController {

    private final MetadataSyncService metadataSyncService;

    public MetadataSyncController(MetadataSyncService metadataSyncService) {
        this.metadataSyncService = metadataSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        try {
            return ResponseEntity.ok(metadataSyncService.sync());
        }
        catch (MetadataSourceNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "METADATA_SOURCE_NOT_CONFIGURED"));
        }
        catch (MssqlNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "MSSQL_NOT_CONFIGURED"));
        }
        catch (MssqlUnavailableException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "MSSQL_UNAVAILABLE"));
        }
    }
}
