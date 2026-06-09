package com.searchengine.crawler;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class CrawlResult {

    String url;
    String title;
    String text;
    Set<String> outboundLinks;
    boolean success;
    String errorMessage;

    public static CrawlResult failure(String url, String reason) {
        return CrawlResult.builder()
            .url(url)
            .success(false)
            .errorMessage(reason)
            .outboundLinks(Set.of())
            .build();
    }
}
