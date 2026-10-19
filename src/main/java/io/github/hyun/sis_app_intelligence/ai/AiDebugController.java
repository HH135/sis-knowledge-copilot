package io.github.hyun.sis_app_intelligence.ai;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("portfolio")
@RequestMapping("/api/ai")
public class AiDebugController {

    private final AiAnalysisPort aiAnalysisPort;

    public AiDebugController(AiAnalysisPort aiAnalysisPort) {
        this.aiAnalysisPort = aiAnalysisPort;
    }

    @PostMapping("/analyze-debug")
    public AnalysisResult analyze(@Valid @RequestBody AnalysisRequest request) {
        return aiAnalysisPort.analyze(request);
    }
}
