package io.github.hyun.sis_app_intelligence.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.hyun.sis_app_intelligence.metadata.ProcedureMeta;
import io.github.hyun.sis_app_intelligence.source.SourceFile;

@Service
public class ChunkingService {

    private static final Logger LOGGER = Logger.getLogger(ChunkingService.class.getName());
    private static final List<String> FILE_EXTENSIONS = List.of(".java", ".jsp", ".properties", ".ini", ".xml");

    private final DocumentChunkRepository repository;
    private final LineWindowChunker lineWindowChunker;
    private final MapperStatementChunker mapperStatementChunker;
    private final ProcedureChunker procedureChunker;
    private final TableChunker tableChunker;
    private final Environment environment;

    public ChunkingService(
            DocumentChunkRepository repository,
            LineWindowChunker lineWindowChunker,
            MapperStatementChunker mapperStatementChunker,
            ProcedureChunker procedureChunker,
            TableChunker tableChunker,
            Environment environment) {
        this.repository = repository;
        this.lineWindowChunker = lineWindowChunker;
        this.mapperStatementChunker = mapperStatementChunker;
        this.procedureChunker = procedureChunker;
        this.tableChunker = tableChunker;
        this.environment = environment;
    }

    @Transactional
    public ChunkResult rebuild() {
        String origin = environment.matchesProfiles("internal") ? "internal" : "portfolio";
        repository.deleteByOrigin(origin);

        Map<String, Integer> byKind = new LinkedHashMap<>();
        byKind.put("FILE", 0);
        byKind.put("MAPPER_STATEMENT", 0);
        byKind.put("PROCEDURE", 0);
        byKind.put("TABLE", 0);
        int mapperFallbacks = 0;

        for (SourceFile file : repository.findSourceFiles(origin)) {
            String extension = extension(file.relativePath());
            if (!FILE_EXTENSIONS.contains(extension) || (origin.equals("portfolio") && extension.equals(".sql"))) {
                continue;
            }
            List<ChunkDraft> chunks;
            if (extension.equals(".xml") && isMapperPath(file.relativePath())) {
                try {
                    chunks = mapperStatementChunker.chunk(input(origin, "MAPPER_STATEMENT",
                            file.relativePath(), file.relativePath(), file.content(), null, null));
                    add(byKind, "MAPPER_STATEMENT", chunks);
                    continue;
                }
                catch (MapperStatementChunker.MapperParseException exception) {
                    mapperFallbacks++;
                }
            }
            chunks = lineWindowChunker.chunk(input(
                    origin, "FILE", file.relativePath(), file.relativePath(), file.content(), null, null));
            add(byKind, "FILE", chunks);
        }

        for (ProcedureMeta procedure : repository.findProcedures()) {
            String title = procedure.schemaName() + "." + procedure.procedureName();
            String sourceRef = procedure.sourceRef() == null || procedure.sourceRef().isBlank()
                    ? title
                    : procedure.sourceRef();
            List<ChunkDraft> chunks = procedureChunker.chunk(input(
                    origin,
                    "PROCEDURE",
                    sourceRef,
                    title,
                    procedure.definition(),
                    procedure.description(),
                    procedure.definitionStatus()));
            add(byKind, "PROCEDURE", chunks);
        }

        for (DocumentChunkRepository.TableChunkData table : repository.findTables()) {
            String title = table.schemaName() + "." + table.tableName();
            List<ChunkDraft> chunks = tableChunker.chunk(input(
                    origin,
                    "TABLE",
                    title,
                    title,
                    table.columnsText(),
                    table.description(),
                    null));
            add(byKind, "TABLE", chunks);
        }

        int total = byKind.values().stream().mapToInt(Integer::intValue).sum();
        LOGGER.info("Chunk rebuild completed: origin=" + origin + ", total=" + total
                + ", byKind=" + byKind + ", mapperFallbacks=" + mapperFallbacks);
        return new ChunkResult(origin, total, Map.copyOf(byKind), mapperFallbacks);
    }

    private void add(Map<String, Integer> counts, String kind, List<ChunkDraft> chunks) {
        chunks.forEach(repository::insert);
        counts.compute(kind, (ignored, count) -> count + chunks.size());
    }

    private ChunkingInput input(
            String origin,
            String kind,
            String sourceRef,
            String title,
            String content,
            String description,
            String definitionStatus) {
        return new ChunkingInput(origin, kind, sourceRef, title, content, description, definitionStatus);
    }

    private boolean isMapperPath(String path) {
        return path.toLowerCase(Locale.ROOT).replace('\\', '/').contains("/mapper/");
    }

    private String extension(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        int separator = normalized.lastIndexOf('/');
        int dot = normalized.lastIndexOf('.');
        return dot > separator ? normalized.substring(dot) : "";
    }
}
