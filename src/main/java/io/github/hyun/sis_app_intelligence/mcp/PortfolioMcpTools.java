package io.github.hyun.sis_app_intelligence.mcp;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import io.github.hyun.sis_app_intelligence.dependency.CallTreeNode;
import io.github.hyun.sis_app_intelligence.dependency.DependencyService;
import io.github.hyun.sis_app_intelligence.metadata.ColumnSearchItem;
import io.github.hyun.sis_app_intelligence.metadata.MetadataSearchService;
import io.github.hyun.sis_app_intelligence.metadata.ProcedureSource;
import io.github.hyun.sis_app_intelligence.metadata.SearchResponse;

@Component
@Profile("portfolio")
public class PortfolioMcpTools {

    private static final int DEFAULT_CALL_TREE_DEPTH = 5;
    private static final int MAX_CALL_TREE_DEPTH = 10;
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");

    private final MetadataSearchService metadataSearchService;
    private final DependencyService dependencyService;

    public PortfolioMcpTools(
            MetadataSearchService metadataSearchService,
            DependencyService dependencyService) {
        this.metadataSearchService = metadataSearchService;
        this.dependencyService = dependencyService;
    }

    @McpTool(
            name = "search_columns",
            description = "Search fictional portfolio table and column metadata.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public SearchResponse<ColumnSearchItem> searchColumns(
            @McpToolParam(description = "A table, column, or description search term.") String query,
            @McpToolParam(description = "Optional exact table name.", required = false) String table,
            @McpToolParam(description = "Optional result count from 1 to 200.", required = false) Integer limit) {
        return metadataSearchService.searchColumns(query, table, limit);
    }

    @McpTool(
            name = "get_procedure_source",
            description = "Read one fictional stored procedure by exact schema and name.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ProcedureSource getProcedureSource(
            @McpToolParam(description = "Procedure schema, for example dbo.") String schemaName,
            @McpToolParam(description = "Exact procedure name, for example USP_COMPLETE_REPAIR_ORDER.")
            String procedureName) {
        requireIdentifier(schemaName, "schema");
        requireIdentifier(procedureName, "procedure");
        return metadataSearchService.findProcedureSource(schemaName, procedureName)
                .orElseThrow(() -> new IllegalArgumentException("Procedure was not found."));
    }

    @McpTool(
            name = "get_call_tree",
            description = "Read the bounded call tree for one fictional stored procedure.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public CallTreeNode getCallTree(
            @McpToolParam(description = "Schema-qualified procedure name, for example dbo.USP_COMPLETE_REPAIR_ORDER.")
            String procedure,
            @McpToolParam(description = "Maximum traversal depth from 0 to 10; defaults to 5.", required = false)
            Integer depth) {
        String[] parts = procedure == null ? new String[0] : procedure.split("\\.", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Procedure must be schema-qualified.");
        }
        requireIdentifier(parts[0], "schema");
        requireIdentifier(parts[1], "procedure");

        int resolvedDepth = depth == null ? DEFAULT_CALL_TREE_DEPTH : depth;
        if (resolvedDepth < 0 || resolvedDepth > MAX_CALL_TREE_DEPTH) {
            throw new IllegalArgumentException("Depth must be between 0 and 10.");
        }
        return dependencyService.callTree(procedure, resolvedDepth);
    }

    private void requireIdentifier(String value, String field) {
        if (value == null || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + field + " identifier.");
        }
    }
}
