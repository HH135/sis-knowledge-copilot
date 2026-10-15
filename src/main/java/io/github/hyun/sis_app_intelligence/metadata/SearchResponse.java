package io.github.hyun.sis_app_intelligence.metadata;

import java.util.List;

public record SearchResponse<T>(
        String query,
        int count,
        List<T> items
) {
    public SearchResponse(String query, List<T> items) {
        this(query, items.size(), List.copyOf(items));
    }
}
