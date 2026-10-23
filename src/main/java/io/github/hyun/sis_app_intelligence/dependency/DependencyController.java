package io.github.hyun.sis_app_intelligence.dependency;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {

    private static final int DEFAULT_DEPTH = 5;
    private static final int MAX_DEPTH = 10;

    private final DependencyService dependencyService;

    public DependencyController(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @PostMapping("/rebuild")
    public DependencyRebuildResult rebuild() {
        return dependencyService.rebuild();
    }

    @GetMapping("/call-tree")
    public ResponseEntity<?> callTree(
            @RequestParam String procedure,
            @RequestParam(required = false) Integer depth) {
        int resolvedDepth = depth == null ? DEFAULT_DEPTH : depth;
        if (resolvedDepth < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_DEPTH"));
        }
        try {
            return ResponseEntity.ok(dependencyService.callTree(procedure, Math.min(resolvedDepth, MAX_DEPTH)));
        }
        catch (ProcedureNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "PROCEDURE_NOT_FOUND"));
        }
    }

    @GetMapping("/callers")
    public ResponseEntity<?> callers(@RequestParam String procedure) {
        try {
            return ResponseEntity.ok(Map.of(
                    "procedure", procedure,
                    "items", dependencyService.callers(procedure)));
        }
        catch (ProcedureNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "PROCEDURE_NOT_FOUND"));
        }
    }
}
