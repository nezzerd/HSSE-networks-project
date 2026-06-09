package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageFetcherTest {

    private PageFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new PageFetcher(new CrawlerProperties());
    }

    @Test
    void fetch_rejectsLocalhostSsrf() {
        CrawlResult result = fetcher.fetch("http://localhost/admin");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).containsIgnoringCase("SSRF");
    }

    @Test
    void fetch_rejectsPrivateIp() {
        CrawlResult result = fetcher.fetch("http://192.168.1.1/");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void fetch_rejectsFileScheme() {
        CrawlResult result = fetcher.fetch("file:///etc/passwd");
        assertThat(result.isSuccess()).isFalse();
    }
}
