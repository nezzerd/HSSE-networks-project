package com.searchengine.indexer;

public record SearchHit(
    long pageId,
    String url,
    String title,
    String snippet,
    float score
) {}
