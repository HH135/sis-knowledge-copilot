package io.github.hyun.sis_app_intelligence.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class MockAiAnalysisAdapter implements AiAnalysisPort {

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        String titles = request.contexts().stream()
                .map(ContextChunk::title)
                .collect(Collectors.joining(", "));
        String answer = "[MOCK] " + request.question().trim() + " + " + titles;
        List<Citation> citations = request.contexts().stream()
                .map(context -> new Citation(
                        context.sourceKind(),
                        context.sourceRef(),
                        context.title(),
                        context.startLine(),
                        context.endLine()))
                .toList();
        return new AnalysisResult(answer, citations, providerName());
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public boolean sendsDataExternally() {
        return false;
    }
}
