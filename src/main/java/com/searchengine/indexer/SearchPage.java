package com.searchengine.indexer;

import java.util.List;

public record SearchPage(
    List<SearchHit> hits,
    boolean hasMore
) {
    public static SearchPage empty() {
        return new SearchPage(List.of(), false);
    }
}
