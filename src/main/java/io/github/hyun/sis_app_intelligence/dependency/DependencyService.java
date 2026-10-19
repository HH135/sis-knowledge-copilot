package io.github.hyun.sis_app_intelligence.dependency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.hyun.sis_app_intelligence.metadata.ProcedureMeta;
import io.github.hyun.sis_app_intelligence.source.SourceFile;

@Service
public class DependencyService {

    private static final Logger LOGGER = Logger.getLogger(DependencyService.class.getName());

    private final DependencyRepository repository;
    private final ExecCallParser execCallParser;
    private final MapperCallParser mapperCallParser;
    private final Environment environment;

    public DependencyService(
            DependencyRepository repository,
            ExecCallParser execCallParser,
            MapperCallParser mapperCallParser,
            Environment environment) {
        this.repository = repository;
        this.execCallParser = execCallParser;
        this.mapperCallParser = mapperCallParser;
        this.environment = environment;
    }

    @Transactional
    public DependencyRebuildResult rebuild() {
        String origin = activeOrigin();
        List<ProcedureMeta> procedures = repository.findProcedures();
        Set<String> knownProcedures = new HashSet<>();
        for (ProcedureMeta procedure : procedures) {
            knownProcedures.add(normalizeProcedure(procedure.schemaName(), procedure.procedureName()));
        }

        repository.deleteByOrigin(origin);
        Map<String, Integer> byType = new LinkedHashMap<>();
        byType.put("RESOLVED", 0);
        byType.put("MISSING", 0);
        byType.put("DYNAMIC", 0);
        byType.put("SYSTEM", 0);
        int mapperEdges = 0;

        for (ProcedureMeta procedure : procedures) {
            String callerRef = procedure.schemaName() + "." + procedure.procedureName();
            for (ParsedCall call : execCallParser.parse(procedure.definition())) {
                DependencyEdge edge = resolve(origin, "PROCEDURE", callerRef, call, knownProcedures);
                repository.insert(edge);
                byType.compute(edge.callType(), (ignored, count) -> count + 1);
            }
        }

        for (SourceFile mapper : repository.findMapperFiles(origin)) {
            for (MapperCallParser.MapperCall call : mapperCallParser.parse(mapper.relativePath(), mapper.content())) {
                DependencyEdge edge = resolve(
                        origin, "MAPPER_STATEMENT", call.callerRef(), call.parsedCall(), knownProcedures);
                repository.insert(edge);
                byType.compute(edge.callType(), (ignored, count) -> count + 1);
                mapperEdges++;
            }
        }

        int edgeCount = byType.values().stream().mapToInt(Integer::intValue).sum();
        LOGGER.info("Dependency rebuild completed: origin=" + origin + ", edges=" + edgeCount
                + ", mapperEdges=" + mapperEdges + ", byType=" + byType);
        return new DependencyRebuildResult(origin, edgeCount, Map.copyOf(byType), mapperEdges);
    }

    public CallTreeNode callTree(String procedure, int depth) {
        String origin = activeOrigin();
        requireProcedure(origin, procedure);
        ProcedureKey root = parseProcedure(procedure);
        Set<String> path = new HashSet<>();
        path.add(root.normalized());
        return new CallTreeNode(root.canonical(), "ROOT", null, false,
                buildChildren(origin, root.canonical(), depth, path));
    }

    public List<CallerItem> callers(String procedure) {
        String origin = activeOrigin();
        requireProcedure(origin, procedure);
        return repository.findCallers(origin, procedure);
    }

    private List<CallTreeNode> buildChildren(String origin, String caller, int remainingDepth, Set<String> path) {
        if (remainingDepth <= 0) {
            return List.of();
        }
        List<CallTreeNode> children = new ArrayList<>();
        for (DependencyEdge edge : repository.findOutgoing(origin, caller)) {
            String callee = edge.calleeSchema() + "." + edge.calleeName();
            String normalized = normalize(callee);
            boolean cycle = edge.callType().equals("RESOLVED") && path.contains(normalized);
            List<CallTreeNode> descendants = List.of();
            if (edge.callType().equals("RESOLVED") && !cycle && remainingDepth > 1) {
                Set<String> nextPath = new HashSet<>(path);
                nextPath.add(normalized);
                descendants = buildChildren(origin, callee, remainingDepth - 1, nextPath);
            }
            children.add(new CallTreeNode(callee, edge.callType(), edge.lineNo(), cycle, descendants));
        }
        return List.copyOf(children);
    }

    private DependencyEdge resolve(
            String origin,
            String callerType,
            String callerRef,
            ParsedCall call,
            Set<String> knownProcedures) {
        String callType = switch (call.kind()) {
            case "DYNAMIC" -> "DYNAMIC";
            case "SYSTEM" -> "SYSTEM";
            default -> knownProcedures.contains(normalizeProcedure(call.schema(), call.name()))
                    ? "RESOLVED"
                    : "MISSING";
        };
        return new DependencyEdge(origin, callerType, callerRef, call.schema(), call.name(), callType, call.lineNo());
    }

    private void requireProcedure(String origin, String procedure) {
        if (procedure == null || procedure.isBlank() || !repository.procedureExists(origin, procedure)) {
            throw new ProcedureNotFoundException();
        }
    }

    private String activeOrigin() {
        return environment.matchesProfiles("internal") ? "internal" : "portfolio";
    }

    private ProcedureKey parseProcedure(String procedure) {
        String value = procedure.trim();
        int separator = value.lastIndexOf('.');
        String schema = separator < 0 ? "dbo" : stripBrackets(value.substring(0, separator));
        String name = stripBrackets(separator < 0 ? value : value.substring(separator + 1));
        return new ProcedureKey(schema, name);
    }

    private String normalizeProcedure(String schema, String name) {
        return normalize(schema + "." + name);
    }

    private String normalize(String value) {
        return value.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
    }

    private String stripBrackets(String value) {
        return value.trim().replace("[", "").replace("]", "");
    }

    private record ProcedureKey(String schema, String name) {
        private String normalized() {
            return schema.toLowerCase(Locale.ROOT) + "." + name.toLowerCase(Locale.ROOT);
        }

        private String canonical() {
            return schema + "." + name;
        }
    }
}
