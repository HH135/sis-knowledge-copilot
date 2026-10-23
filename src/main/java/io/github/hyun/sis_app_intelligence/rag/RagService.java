package io.github.hyun.sis_app_intelligence.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.github.hyun.sis_app_intelligence.ai.AiAnalysisPort;
import io.github.hyun.sis_app_intelligence.ai.AnalysisProviderException;
import io.github.hyun.sis_app_intelligence.ai.AnalysisRequest;
import io.github.hyun.sis_app_intelligence.ai.AnalysisResult;
import io.github.hyun.sis_app_intelligence.ai.ContextChunk;

@Service
public class RagService {

    private static final Logger LOGGER = Logger.getLogger(RagService.class.getName());
    private static final int DEFAULT_K = 6;

    private final FtsRetriever ftsRetriever;
    private final VectorRetriever vectorRetriever;
    private final HybridRetriever hybridRetriever;
    private final DependencyExpander dependencyExpander;
    private final CitationValidator citationValidator;
    private final AiAnalysisPort aiAnalysisPort;
    private final Environment environment;

    public RagService(
            FtsRetriever ftsRetriever,
            VectorRetriever vectorRetriever,
            HybridRetriever hybridRetriever,
            DependencyExpander dependencyExpander,
            CitationValidator citationValidator,
            AiAnalysisPort aiAnalysisPort,
            Environment environment) {
        this.ftsRetriever = ftsRetriever;
        this.vectorRetriever = vectorRetriever;
        this.hybridRetriever = hybridRetriever;
        this.dependencyExpander = dependencyExpander;
        this.citationValidator = citationValidator;
        this.aiAnalysisPort = aiAnalysisPort;
        this.environment = environment;
    }

    public Map<String, Object> ask(RagAskRequest request) {
        long started = System.nanoTime();
        RetrievalMode requestedMode = request.mode() == null ? RetrievalMode.HYBRID : request.mode();
        RetrievalMode effectiveMode = requestedMode;
        String modeFallback = null;
        if (environment.matchesProfiles("internal") && requestedMode != RetrievalMode.FTS) {
            effectiveMode = RetrievalMode.FTS;
            modeFallback = "FTS";
        }

        String origin = environment.matchesProfiles("internal") ? "internal" : "portfolio";
        int limit = request.k() == null ? DEFAULT_K : request.k();
        List<RetrievedChunk> retrieved = switch (effectiveMode) {
            case FTS -> ftsRetriever.retrieve(origin, request.question()).stream().limit(limit).toList();
            case VECTOR -> vectorRetriever.retrieve(origin, request.question()).stream().limit(limit).toList();
            case HYBRID -> hybridRetriever.retrieve(origin, request.question(), limit);
        };
        List<RetrievedChunk> contexts = dependencyExpander.expand(
                origin, retrieved, Boolean.TRUE.equals(request.expand()));

        List<ContextChunk> analysisContexts = contexts.stream()
                .map(chunk -> new ContextChunk(
                        chunk.sourceKind(),
                        chunk.sourceRef(),
                        chunk.title(),
                        chunk.startLine(),
                        chunk.endLine(),
                        chunk.content()))
                .toList();

        String answer = null;
        String provider = aiAnalysisPort.providerName();
        String lastError = null;
        int invalidCitations = 0;
        List<CitationValidator.CitedContext> citations;
        try {
            AnalysisResult analysis = aiAnalysisPort.analyze(new AnalysisRequest(request.question(), analysisContexts));
            provider = analysis.provider();
            CitationValidator.ValidationResult validation = citationValidator.validate(analysis.answer(), contexts);
            answer = validation.answer();
            invalidCitations = validation.invalidCitations();
            citations = validation.citations();
        }
        catch (AnalysisProviderException exception) {
            lastError = exception.lastError();
            citations = allCitationCandidates(contexts);
        }

        List<Map<String, Object>> retrievalResponse = new ArrayList<>();
        for (int index = 0; index < contexts.size(); index++) {
            RetrievedChunk chunk = contexts.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("no", index + 1);
            item.put("chunkId", chunk.chunkId());
            item.put("title", chunk.title());
            item.put("ftsRank", chunk.ftsRank());
            item.put("vectorRank", chunk.vectorRank());
            item.put("rrfScore", chunk.rrfScore());
            item.put("expanded", chunk.expanded());
            retrievalResponse.add(item);
        }

        List<Map<String, Object>> citationResponse = citations.stream()
                .map(citation -> citationMap(citation.no(), citation.chunk()))
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", request.question());
        response.put("mode", effectiveMode);
        response.put("answer", answer);
        response.put("provider", provider);
        response.put("citations", citationResponse);
        response.put("retrieval", retrievalResponse);
        response.put("invalidCitations", invalidCitations);
        response.put("elapsedMs", (System.nanoTime() - started) / 1_000_000);
        response.put("lastError", lastError);
        if (modeFallback != null) {
            response.put("modeFallback", modeFallback);
        }
        LOGGER.info("RAG request completed: origin=" + origin + ", mode=" + effectiveMode
                + ", retrieval=" + retrieved.size() + ", contexts=" + contexts.size()
                + ", elapsedMs=" + response.get("elapsedMs"));
        return response;
    }

    private List<CitationValidator.CitedContext> allCitationCandidates(List<RetrievedChunk> contexts) {
        List<CitationValidator.CitedContext> candidates = new ArrayList<>();
        for (int index = 0; index < contexts.size(); index++) {
            candidates.add(new CitationValidator.CitedContext(index + 1, contexts.get(index)));
        }
        return candidates;
    }

    private Map<String, Object> citationMap(int number, RetrievedChunk chunk) {
        Map<String, Object> citation = new LinkedHashMap<>();
        citation.put("no", number);
        citation.put("sourceKind", chunk.sourceKind());
        citation.put("title", chunk.title());
        citation.put("sourceRef", chunk.sourceRef());
        citation.put("startLine", chunk.startLine());
        citation.put("endLine", chunk.endLine());
        citation.put("expanded", chunk.expanded());
        citation.put("expandedFrom", chunk.expandedFrom());
        return citation;
    }
}
