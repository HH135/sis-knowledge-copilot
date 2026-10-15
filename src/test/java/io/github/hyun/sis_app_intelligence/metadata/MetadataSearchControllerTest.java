package io.github.hyun.sis_app_intelligence.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MetadataSearchControllerTest {

    private MetadataSearchRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = mock(MetadataSearchRepository.class);
        when(repository.searchColumns(anyString(), anyString(), anyString(), nullable(String.class), anyInt()))
                .thenAnswer(invocation -> {
                    int limit = invocation.getArgument(4);
                    if (limit == 200) {
                        return IntStream.range(0, limit)
                                .mapToObj(index -> new ColumnSearchItem("ACME_SVC", "dbo", "TB_SAMPLE",
                                        "샘플", "COLUMN_" + index, index, "varchar(20)", true, "설명"))
                                .toList();
                    }
                    return List.of(new ColumnSearchItem("ACME_SVC", "dbo", "TB_REPAIR_ORDER",
                            "정비 주문 헤더", "ORDER_NO", 1, "varchar(20)", false, "주문번호"));
                });
        when(repository.searchProcedures(anyString(), anyString(), anyString(), anyBoolean(), anyInt()))
                .thenReturn(List.of(new ProcedureSearchItem("ACME_SVC", "dbo", "USP_REDEEM_COUPON",
                        "쿠폰 사용", "portfolio-data/procedures/dbo.USP_REDEEM_COUPON.sql", "OK", "name")));

        mockMvc = standaloneSetup(new MetadataSearchController(new MetadataSearchService(repository))).build();
    }

    @Test
    void returnsSearchResponseForColumnsAndProcedures() throws Exception {
        mockMvc.perform(get("/api/metadata/columns").param("q", " ORDER_NO "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("ORDER_NO"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].columnName").value("ORDER_NO"))
                .andExpect(jsonPath("$.items[0].databaseName").value("ACME_SVC"));

        mockMvc.perform(get("/api/metadata/procedures").param("q", "COUPON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("COUPON"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].matchedIn").value("name"))
                .andExpect(jsonPath("$.items[0].definitionStatus").value("OK"))
                .andExpect(jsonPath("$.items[0].definition").doesNotExist());
    }

    @Test
    void rejectsMissingBlankLongQueriesAndNonPositiveLimit() throws Exception {
        mockMvc.perform(get("/api/metadata/columns"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QUERY_REQUIRED"));
        mockMvc.perform(get("/api/metadata/columns").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QUERY_REQUIRED"));
        mockMvc.perform(get("/api/metadata/columns").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("QUERY_TOO_LONG"));
        mockMvc.perform(get("/api/metadata/columns").param("q", "ORDER_NO").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_LIMIT"));
    }

    @Test
    void capsLargeLimitAtTwoHundred() throws Exception {
        mockMvc.perform(get("/api/metadata/columns").param("q", "COLUMN").param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(200))
                .andExpect(jsonPath("$.items.length()").value(200));
    }
}
