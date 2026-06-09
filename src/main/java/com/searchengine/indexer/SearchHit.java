package com.searchengine.indexer;

public record SearchHit(
    long pageId,
    String url,
    String title,
    String titleHighlighted,
    String snippet,
    float score
) {}
