package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("portfolio")
class MetadataSearchIntegrationTest {

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private MetadataSearchService metadataSearchService;

    @BeforeEach
    void loadFixtureMetadata() {
        metadataSyncService.sync();
    }

    @Test
    void exactColumnNameRanksFirstAndSearchesAcrossTables() {
        SearchResponse<ColumnSearchItem> response = metadataSearchService.searchColumns("ORDER_NO", null, 50);

        assertTrue(response.count() > 1);
        assertEquals("ORDER_NO", response.items().getFirst().columnName());
        assertTrue(response.items().stream().map(ColumnSearchItem::tableName).distinct().count() > 1);

        SearchResponse<ColumnSearchItem> tableResponse =
                metadataSearchService.searchColumns("ORDER_NO", "tb_repair_order", 50);
        assertEquals(1, tableResponse.count());
        assertEquals("TB_REPAIR_ORDER", tableResponse.items().getFirst().tableName());
    }

    @Test
    void searchesProcedureNamesAndDescriptions() {
        SearchResponse<ProcedureSearchItem> response = metadataSearchService.searchProcedures("COUPON", false, 50);

        assertTrue(response.count() > 0);
        assertTrue(response.items().stream()
                .allMatch(item -> item.matchedIn().equals("name") || item.matchedIn().equals("description")));
    }

    @Test
    void canSearchProcedureDefinitionsWithoutReturningThem() {
        SearchResponse<ProcedureSearchItem> response = metadataSearchService.searchProcedures("TB_COUPON", true, 50);

        assertTrue(response.count() > 0);
        assertTrue(response.items().stream().allMatch(item -> item.matchedIn().equals("body")));
    }

    @Test
    void specificationOnlyColumnIsNotIndexed() {
        SearchResponse<ColumnSearchItem> response =
                metadataSearchService.searchColumns("EMAIL_OPT_IN", null, 50);

        assertEquals(0, response.count());
        assertTrue(response.items().isEmpty());
    }
}
