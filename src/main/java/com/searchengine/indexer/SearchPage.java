package com.searchengine.indexer;

import java.util.List;

public record SearchPage(
    List<SearchHit> hits,
    boolean hasMore,
    long totalHits
) {
    public static SearchPage empty() {
        return new SearchPage(List.of(), false, 0L);
    }
}
