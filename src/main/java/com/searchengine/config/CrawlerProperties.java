package com.searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crawler")
@Getter
@Setter
public class CrawlerProperties {

    private int maxPages = 5000;
    private int maxDepth = 5;
    private long maxPageSizeBytes = 2_097_152L;
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 10_000;
    private int maxRedirects = 5;
    private long politenessDelayMs = 1_000;
    private int threadPoolSize = 8;
    private int maxQueueSize = 100_000;
    private int maxLinksPerPage = 500;
    private String userAgent = "SearchEngineBot/1.0";
    private int simhashThreshold = 3;
    private int simhashCandidates = 50;
}
