package io.github.hyun.sis_app_intelligence.metadata;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class MetadataSearchService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");

    private final MetadataSearchRepository metadataSearchRepository;

    public MetadataSearchService(MetadataSearchRepository metadataSearchRepository) {
        this.metadataSearchRepository = metadataSearchRepository;
    }

    public SearchResponse<ColumnSearchItem> searchColumns(String query, String table, Integer requestedLimit) {
        String normalizedQuery = normalizeQuery(query);
        int limit = normalizeLimit(requestedLimit);
        String escaped = LikePatternEscaper.escape(normalizedQuery);
        List<ColumnSearchItem> items = metadataSearchRepository.searchColumns(
                escaped, escaped + "%", "%" + escaped + "%", normalizeTable(table), limit);
        return new SearchResponse<>(normalizedQuery, items);
    }

    public SearchResponse<ProcedureSearchItem> searchProcedures(
            String query, boolean searchBody, Integer requestedLimit) {
        String normalizedQuery = normalizeQuery(query);
        int limit = normalizeLimit(requestedLimit);
        String escaped = LikePatternEscaper.escape(normalizedQuery);
        List<ProcedureSearchItem> items = metadataSearchRepository.searchProcedures(
                escaped, escaped + "%", "%" + escaped + "%", searchBody, limit);
        return new SearchResponse<>(normalizedQuery, items);
    }

    public Optional<ProcedureSource> findProcedureSource(String schemaName, String procedureName) {
        validateIdentifier(schemaName, "schema");
        validateIdentifier(procedureName, "procedure");
        return metadataSearchRepository.findProcedureSource(schemaName, procedureName);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new MetadataSearchValidationException("QUERY_REQUIRED");
        }

        String normalized = query.trim();
        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw new MetadataSearchValidationException("QUERY_TOO_LONG");
        }
        return normalized;
    }

    private int normalizeLimit(Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1) {
            throw new MetadataSearchValidationException("INVALID_LIMIT");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeTable(String table) {
        return table == null || table.isBlank() ? null : table.trim();
    }

    private void validateIdentifier(String value, String field) {
        if (value == null || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new MetadataSearchValidationException("INVALID_" + field.toUpperCase(Locale.ROOT));
        }
    }
}
