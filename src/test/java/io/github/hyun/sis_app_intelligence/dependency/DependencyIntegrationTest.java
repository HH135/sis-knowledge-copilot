package io.github.hyun.sis_app_intelligence.dependency;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import io.github.hyun.sis_app_intelligence.collector.SourceCollectorService;
import io.github.hyun.sis_app_intelligence.metadata.MetadataSyncService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("portfolio")
@Transactional
class DependencyIntegrationTest {

    private static final Pattern EXPECTED_EDGE = Pattern.compile(
            "\\| `([^`]+)` \\| `([^`]+)` \\| Resolved \\|");

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private SourceCollectorService sourceCollectorService;

    @Autowired
    private DependencyService dependencyService;

    @Autowired
    private DependencyRepository dependencyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rebuildsPortfolioDependenciesAndMatchesGoldenEdges() throws Exception {
        metadataSyncService.sync();
        sourceCollectorService.scan();

        DependencyRebuildResult first = dependencyService.rebuild();
        DependencyRebuildResult second = dependencyService.rebuild();

        assertEquals(first.edges(), second.edges());
        assertEquals(first.byType(), second.byType());
        assertEquals(first.mapperEdges(), second.mapperEdges());
        assertEquals(22, first.byType().get("RESOLVED"));
        assertEquals(1, first.byType().get("MISSING"));
        assertEquals(1, first.byType().get("DYNAMIC"));
        assertEquals(0, first.byType().get("SYSTEM"));
        assertEquals(14, first.mapperEdges());
        assertEquals(expectedResolvedEdges(), databaseResolvedEdges());
        assertTrue(dependencyRepository.findOutgoing(
                "portfolio", "dbo.USP_CREATE_REPAIR_ORDER").stream()
                .anyMatch(edge -> edge.calleeName().equals("USP_LEGACY_SYNC")
                        && edge.callType().equals("MISSING")));
        assertTrue(dependencyRepository.findOutgoing(
                "portfolio", "dbo.USP_RETRY_INTERFACE_SYNC").stream()
                .anyMatch(edge -> edge.calleeName().equals("sp_executesql")
                        && edge.callType().equals("DYNAMIC")));
        assertFalse(dependencyRepository.findOutgoing(
                "portfolio", "dbo.USP_CREATE_REPAIR_ORDER").stream()
                .anyMatch(edge -> edge.calleeName().equals("USP_CONFIRM_APPOINTMENT")));
    }

    @Test
    void callTreeShowsCompletionChainAndMarksCycles() {
        metadataSyncService.sync();
        sourceCollectorService.scan();
        dependencyService.rebuild();

        CallTreeNode tree = dependencyService.callTree("dbo.USP_COMPLETE_REPAIR_ORDER", 5);
        CallTreeNode payment = tree.children().stream()
                .filter(node -> node.procedure().equals("dbo.USP_CONFIRM_PAYMENT"))
                .findFirst().orElseThrow();
        CallTreeNode coupon = payment.children().stream()
                .filter(node -> node.procedure().equals("dbo.USP_REDEEM_COUPON"))
                .findFirst().orElseThrow();
        assertTrue(coupon.children().stream()
                .anyMatch(node -> node.procedure().equals("dbo.USP_WRITE_INTERFACE_LOG")));

        jdbcTemplate.update("""
                INSERT INTO proc_dependency
                    (origin, caller_type, caller_ref, callee_schema, callee_name, call_type, line_no)
                VALUES ('portfolio', 'PROCEDURE', 'dbo.USP_CONFIRM_PAYMENT',
                        'dbo', 'USP_COMPLETE_REPAIR_ORDER', 'RESOLVED', 999)
                """);
        CallTreeNode cyclicTree = dependencyService.callTree("dbo.USP_COMPLETE_REPAIR_ORDER", 5);
        CallTreeNode cyclicPayment = cyclicTree.children().stream()
                .filter(node -> node.procedure().equals("dbo.USP_CONFIRM_PAYMENT"))
                .findFirst().orElseThrow();
        assertTrue(cyclicPayment.children().stream()
                .anyMatch(node -> node.procedure().equals("dbo.USP_COMPLETE_REPAIR_ORDER") && node.cycle()));
    }

    @Test
    void callersIncludesProcedureAndMapperCallers() {
        metadataSyncService.sync();
        sourceCollectorService.scan();
        dependencyService.rebuild();

        var callers = dependencyService.callers("dbo.USP_WRITE_INTERFACE_LOG");

        assertTrue(callers.stream().anyMatch(caller -> caller.callerType().equals("PROCEDURE")));
        assertTrue(callers.stream().anyMatch(caller -> caller.callerType().equals("MAPPER_STATEMENT")
                && caller.callerRef().equals(
                        "portfolio-data/app/resources/mapper/CommonMapper.xml#writeLog")));
        assertTrue(callers.size() > 1);
    }

    private Set<String> expectedResolvedEdges() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        String markdown;
        try (var input = resolver.getResource(
                "classpath:portfolio-data/procedures/EXPECTED_CALLS.md").getInputStream()) {
            markdown = StreamUtils.copyToString(input, java.nio.charset.StandardCharsets.UTF_8);
        }
        Matcher matcher = EXPECTED_EDGE.matcher(markdown);
        Set<String> edges = new HashSet<>();
        while (matcher.find()) {
            edges.add(matcher.group(1) + "->" + matcher.group(2));
        }
        return edges;
    }

    private Set<String> databaseResolvedEdges() {
        return jdbcTemplate.query("""
                SELECT caller_ref, callee_schema, callee_name
                FROM proc_dependency
                WHERE origin = 'portfolio' AND caller_type = 'PROCEDURE' AND call_type = 'RESOLVED'
                """, resultSet -> {
            Set<String> edges = new HashSet<>();
            while (resultSet.next()) {
                edges.add(resultSet.getString("caller_ref") + "->"
                        + resultSet.getString("callee_schema") + "." + resultSet.getString("callee_name"));
            }
            return edges;
        });
    }
}
